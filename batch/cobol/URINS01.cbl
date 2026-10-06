       IDENTIFICATION DIVISION.
       PROGRAM-ID. URINS01.
      ******************************************************************
      * うさぎ鉄道 車両保守 (検修) 日次バッチ                            *
      * URINS01 : 編成別 検査期限 算出                                   *
      *                                                                *
      * 入力 : FORMATIONS.DAT  編成検査実績 抽出ファイル                  *
      *          H : 'H' + 基準日(8)                                   *
      *          D : 'D' + 編成(6) + 前回交番(8) + 前回重要部(8)          *
      *                  + 前回全般(8) + 重要部後走行km(7)               *
      *          T : 'T' + 件数(6)                                     *
      * 出力 : INSPDUE.DAT     検査期限ファイル (検修計画システムへ連携)  *
      *          H : 'H' + 基準日(8)                                   *
      *          D : 'D' + 編成(6) + 交番期限(8) + 重要部期限(8)          *
      *                  + 全般期限(8) + 次回種別(1) + 次回期限(8)        *
      *                  + 残日数符号(1) + 残日数(4) + 走行km超過(1)      *
      *                  + 判定(1)                                     *
      *          T : 'T' + 件数(6) + 注意件数(6) + 超過件数(6)           *
      *                                                                *
      * 検査周期 (URMS 検修規程 第12条, 架空の社内基準)                   *
      *   交番検査   K : 前回 + 90 日                                   *
      *   重要部検査 J : 前回 + 4 年 または 走行 60 万 km                 *
      *   全般検査   Z : 前回 + 8 年                                    *
      *   年加算で 2/29 が存在しない場合は 2/28 とする                    *
      * 判定                                                            *
      *   X 超過 : 残日数 < 0 または 走行 60 万 km 以上                   *
      *   W 注意 : 残日数 <= 14 または 走行 57 万 km 以上                 *
      *   N 正常 : 上記以外                                             *
      *                                                                *
      * 変更履歴                                                        *
      *   2013/10/01 初版                     (うさぎ電機システムズ)      *
      *   2017/04/01 走行 km 条件 追加 (重要部)  (高橋)                   *
      *   2020/03/02 うるう日 補正 追加           (高橋)                  *
      ******************************************************************
       ENVIRONMENT DIVISION.
       INPUT-OUTPUT SECTION.
       FILE-CONTROL.
           SELECT FORM-FILE ASSIGN TO "FORMATIONS.DAT"
               ORGANIZATION IS LINE SEQUENTIAL
               FILE STATUS IS WS-IN-STATUS.
           SELECT OUT-FILE ASSIGN TO "INSPDUE.DAT"
               ORGANIZATION IS LINE SEQUENTIAL
               FILE STATUS IS WS-OUT-STATUS.

       DATA DIVISION.
       FILE SECTION.
       FD  FORM-FILE.
       01  FORM-REC                      PIC X(40).

       FD  OUT-FILE.
       01  OUT-REC                     PIC X(48).

       WORKING-STORAGE SECTION.
       01  WS-IN-STATUS                PIC X(02).
       01  WS-OUT-STATUS               PIC X(02).
       01  WS-EOF                      PIC X(01) VALUE "N".

      *    検査周期 定数
       01  WS-RULE.
           05  WS-KOBAN-DAYS           PIC 9(03) VALUE 90.
           05  WS-JUYOBU-YEARS         PIC 9(02) VALUE 4.
           05  WS-ZENPAN-YEARS         PIC 9(02) VALUE 8.
           05  WS-JUYOBU-KM-LIMIT      PIC 9(07) VALUE 600000.
           05  WS-JUYOBU-KM-WARN       PIC 9(07) VALUE 570000.
           05  WS-WARN-DAYS            PIC 9(03) VALUE 14.

       01  WS-IN-HDR.
           05  WS-IH-KIND              PIC X(01).
           05  WS-IH-DATE              PIC 9(08).
           05  FILLER                  PIC X(31).

       01  WS-IN-DTL.
           05  WS-ID-KIND              PIC X(01).
           05  WS-ID-FORMATION         PIC X(06).
           05  WS-ID-KOBAN             PIC 9(08).
           05  WS-ID-JUYOBU            PIC 9(08).
           05  WS-ID-ZENPAN            PIC 9(08).
           05  WS-ID-KM                PIC 9(07).
           05  FILLER                  PIC X(02).

       01  WS-IN-TRL.
           05  WS-IT-KIND              PIC X(01).
           05  WS-IT-COUNT             PIC 9(06).
           05  FILLER                  PIC X(33).

       01  WS-OUT-HDR.
           05  WS-OH-KIND              PIC X(01) VALUE "H".
           05  WS-OH-DATE              PIC 9(08).

       01  WS-OUT-DTL.
           05  WS-OD-KIND              PIC X(01) VALUE "D".
           05  WS-OD-FORMATION         PIC X(06).
           05  WS-OD-KOBAN-DUE         PIC 9(08).
           05  WS-OD-JUYOBU-DUE        PIC 9(08).
           05  WS-OD-ZENPAN-DUE        PIC 9(08).
           05  WS-OD-NEXT-KIND         PIC X(01).
           05  WS-OD-NEXT-DUE          PIC 9(08).
           05  WS-OD-SIGN              PIC X(01).
           05  WS-OD-DAYS              PIC 9(04).
           05  WS-OD-KM-FLAG           PIC X(01).
           05  WS-OD-JUDGE             PIC X(01).

       01  WS-OUT-TRL.
           05  WS-OT-KIND              PIC X(01) VALUE "T".
           05  WS-OT-COUNT             PIC 9(06).
           05  WS-OT-WARN              PIC 9(06).
           05  WS-OT-OVER              PIC 9(06).

       01  WS-CALC.
           05  WS-BASE-DATE            PIC 9(08).
           05  WS-BASE-INT             PIC S9(09).
           05  WS-DAYS                 PIC S9(05).
           05  WS-ABS-DAYS             PIC 9(05).
           05  WS-YEAR                 PIC 9(04).
           05  WS-MMDD                 PIC 9(04).
           05  WS-SRC-DATE             PIC 9(08).
           05  WS-ADD-YEARS            PIC 9(02).
           05  WS-RESULT-DATE          PIC 9(08).
           05  WS-LEAP                 PIC X(01).
           05  WS-COUNT                PIC 9(06) VALUE 0.
           05  WS-WARN-CNT             PIC 9(06) VALUE 0.
           05  WS-OVER-CNT             PIC 9(06) VALUE 0.

       PROCEDURE DIVISION.
       MAIN-PROC.
           OPEN INPUT FORM-FILE
           IF WS-IN-STATUS NOT = "00"
               DISPLAY "URINS01 E: FORMATIONS.DAT OPEN ERROR "
                       WS-IN-STATUS
               MOVE 12 TO RETURN-CODE
               STOP RUN
           END-IF
           OPEN OUTPUT OUT-FILE

           PERFORM UNTIL WS-EOF = "Y"
               READ FORM-FILE
                   AT END
                       MOVE "Y" TO WS-EOF
                   NOT AT END
                       PERFORM DISPATCH-REC
               END-READ
           END-PERFORM

           MOVE WS-COUNT    TO WS-OT-COUNT
           MOVE WS-WARN-CNT TO WS-OT-WARN
           MOVE WS-OVER-CNT TO WS-OT-OVER
           MOVE SPACES TO OUT-REC
           MOVE WS-OUT-TRL TO OUT-REC
           WRITE OUT-REC

           CLOSE FORM-FILE OUT-FILE
           DISPLAY "URINS01 I: 基準日=" WS-BASE-DATE
                   " 編成=" WS-COUNT
                   " 注意=" WS-WARN-CNT
                   " 超過=" WS-OVER-CNT
           IF WS-OVER-CNT > 0
               MOVE 4 TO RETURN-CODE
           ELSE
               MOVE 0 TO RETURN-CODE
           END-IF
           STOP RUN.

       DISPATCH-REC.
           EVALUATE FORM-REC(1:1)
               WHEN "H"
                   MOVE FORM-REC TO WS-IN-HDR
                   MOVE WS-IH-DATE TO WS-BASE-DATE WS-OH-DATE
                   COMPUTE WS-BASE-INT =
                       FUNCTION INTEGER-OF-DATE(WS-BASE-DATE)
                   MOVE SPACES TO OUT-REC
                   MOVE WS-OUT-HDR TO OUT-REC
                   WRITE OUT-REC
               WHEN "D"
                   MOVE FORM-REC TO WS-IN-DTL
                   PERFORM CALC-DUE
               WHEN "T"
                   MOVE FORM-REC TO WS-IN-TRL
                   IF WS-IT-COUNT NOT = WS-COUNT
                       DISPLAY "URINS01 E: トレーラ件数不一致"
                       MOVE 8 TO RETURN-CODE
                       STOP RUN
                   END-IF
               WHEN OTHER
                   DISPLAY "URINS01 W: 不明なレコード区分 "
                           FORM-REC(1:1)
           END-EVALUATE.

       CALC-DUE.
           ADD 1 TO WS-COUNT
           MOVE WS-ID-FORMATION TO WS-OD-FORMATION

      *    交番検査
           COMPUTE WS-OD-KOBAN-DUE = FUNCTION DATE-OF-INTEGER(
               FUNCTION INTEGER-OF-DATE(WS-ID-KOBAN) + WS-KOBAN-DAYS)

      *    重要部検査
           MOVE WS-ID-JUYOBU    TO WS-SRC-DATE
           MOVE WS-JUYOBU-YEARS TO WS-ADD-YEARS
           PERFORM ADD-YEARS
           MOVE WS-RESULT-DATE  TO WS-OD-JUYOBU-DUE

      *    全般検査
           MOVE WS-ID-ZENPAN    TO WS-SRC-DATE
           MOVE WS-ZENPAN-YEARS TO WS-ADD-YEARS
           PERFORM ADD-YEARS
           MOVE WS-RESULT-DATE  TO WS-OD-ZENPAN-DUE

      *    次回検査 (同日の場合は K, J, Z の順で優先)
           MOVE "K" TO WS-OD-NEXT-KIND
           MOVE WS-OD-KOBAN-DUE TO WS-OD-NEXT-DUE
           IF WS-OD-JUYOBU-DUE < WS-OD-NEXT-DUE
               MOVE "J" TO WS-OD-NEXT-KIND
               MOVE WS-OD-JUYOBU-DUE TO WS-OD-NEXT-DUE
           END-IF
           IF WS-OD-ZENPAN-DUE < WS-OD-NEXT-DUE
               MOVE "Z" TO WS-OD-NEXT-KIND
               MOVE WS-OD-ZENPAN-DUE TO WS-OD-NEXT-DUE
           END-IF

           COMPUTE WS-DAYS =
               FUNCTION INTEGER-OF-DATE(WS-OD-NEXT-DUE) - WS-BASE-INT
           IF WS-DAYS < 0
               MOVE "-" TO WS-OD-SIGN
               COMPUTE WS-ABS-DAYS = 0 - WS-DAYS
           ELSE
               MOVE "+" TO WS-OD-SIGN
               MOVE WS-DAYS TO WS-ABS-DAYS
           END-IF
           MOVE WS-ABS-DAYS TO WS-OD-DAYS

           IF WS-ID-KM >= WS-JUYOBU-KM-LIMIT
               MOVE "K" TO WS-OD-KM-FLAG
           ELSE
               MOVE " " TO WS-OD-KM-FLAG
           END-IF

           EVALUATE TRUE
               WHEN WS-DAYS < 0
               WHEN WS-ID-KM >= WS-JUYOBU-KM-LIMIT
                   MOVE "X" TO WS-OD-JUDGE
                   ADD 1 TO WS-OVER-CNT
               WHEN WS-DAYS <= WS-WARN-DAYS
               WHEN WS-ID-KM >= WS-JUYOBU-KM-WARN
                   MOVE "W" TO WS-OD-JUDGE
                   ADD 1 TO WS-WARN-CNT
               WHEN OTHER
                   MOVE "N" TO WS-OD-JUDGE
           END-EVALUATE

           MOVE SPACES TO OUT-REC
           MOVE WS-OUT-DTL TO OUT-REC
           WRITE OUT-REC.

      *    WS-SRC-DATE に WS-ADD-YEARS 年を加算し WS-RESULT-DATE へ
       ADD-YEARS.
           COMPUTE WS-YEAR = WS-SRC-DATE / 10000 + WS-ADD-YEARS
           MOVE WS-SRC-DATE(5:4) TO WS-MMDD
           IF WS-MMDD = 0229
               MOVE "N" TO WS-LEAP
               IF FUNCTION MOD(WS-YEAR, 4) = 0
                  AND FUNCTION MOD(WS-YEAR, 100) NOT = 0
                   MOVE "Y" TO WS-LEAP
               END-IF
               IF FUNCTION MOD(WS-YEAR, 400) = 0
                   MOVE "Y" TO WS-LEAP
               END-IF
               IF WS-LEAP = "N"
                   MOVE 0228 TO WS-MMDD
               END-IF
           END-IF
           COMPUTE WS-RESULT-DATE = WS-YEAR * 10000 + WS-MMDD.
