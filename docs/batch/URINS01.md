# URINS01 編成別 検査期限算出バッチ 仕様書 (COBOL からの逆生成)

| 項目 | 内容 |
|---|---|
| プログラム ID | `URINS01` |
| ソース | [`batch/cobol/URINS01.cbl`](../../batch/cobol/URINS01.cbl) (GnuCOBOL, 約 270 行) |
| 実行スクリプト | [`batch/cobol/run.sh`](../../batch/cobol/run.sh) |
| 業務 | 車両保守 (検修) 日次バッチ。編成ごとに交番・重要部・全般検査の期限と次回検査を算出し、判定 (正常/注意/超過) を付ける |
| 入力 | `FORMATIONS.DAT` 編成検査実績 抽出ファイル |
| 出力 | `INSPDUE.DAT` 検査期限ファイル (検修計画システムへ連携) |
| 根拠規程 | URMS 検修規程 第 12 条 (架空の社内基準) |
| 本書の位置づけ | Java 化 (COBOL → Java) の**契約**。Java 版はここに書かれた挙動をバイト単位で再現すること |

本書の記述はすべて COBOL ソースの読解と、GnuCOBOL 3.1.2 での実行結果 (付録 A) に基づく。ソース中の行番号は `URINS01.cbl` のもの。

変更履歴 (ソースのコメントより, L30-33):

| 日付 | 内容 | 担当 |
|---|---|---|
| 2013/10/01 | 初版 | うさぎ電機システムズ |
| 2017/04/01 | 走行 km 条件 追加 (重要部) | 高橋 |
| 2020/03/02 | うるう日 補正 追加 | 高橋 |

---

## 1. 処理概要

```
OPEN INPUT FORMATIONS.DAT            ── 失敗 → "E: ... OPEN ERROR" / RC=12 / 即終了
OPEN OUTPUT INSPDUE.DAT
1 レコードずつ READ (EOF まで)
  先頭 1 桁で振り分け (DISPATCH-REC)
    'H' → 基準日を保持し、H レコードを出力
    'D' → 検査期限を算出し (CALC-DUE)、D レコードを出力
    'T' → 件数を検証。不一致 → "E: トレーラ件数不一致" / RC=8 / 即終了
    他  → "W: 不明なレコード区分 x" を表示して読み飛ばし
EOF 後、T レコード (件数・注意件数・超過件数) を出力
CLOSE
"I: 基準日=... 編成=... 注意=... 超過=..." を表示
超過件数 > 0 なら RC=4、それ以外 RC=0
```

- 入力の T レコードは「検証するだけ」で、出力の T レコードはプログラム内部のカウンタから EOF 後に作る。
- T レコード読込後も処理は止まらない。T の後ろに D があれば処理・出力され、件数にも加算される (付録 A ケース 04)。
- T レコードが無くてもエラーにならない (件数検証が行われないだけ。ケース 03)。

## 2. ファイル仕様

両ファイルとも `ORGANIZATION IS LINE SEQUENTIAL` (1 レコード = 1 行, 改行 LF)。文字コードは ASCII の範囲のみ使用。

- **読込時**: 行がレコード長より短い場合、残りは空白で埋められる (GnuCOBOL の LINE SEQUENTIAL の挙動)。
- **書込時**: レコード末尾の空白は出力されない (GnuCOBOL の LINE SEQUENTIAL の挙動)。そのため実際の行長は H=9, D=47, T=19 桁になる (レコード領域 `OUT-REC` は 48 桁だが、D レコードの実データは 47 桁)。

### 2.1 入力 `FORMATIONS.DAT` (レコード長 40 桁, `FORM-REC PIC X(40)`)

**H: ヘッダ** (`WS-IN-HDR`)

| 桁 | 長さ | 項目 | PIC | 内容 |
|---|---|---|---|---|
| 1 | 1 | レコード区分 | X(01) | `H` |
| 2-9 | 8 | 基準日 | 9(08) | YYYYMMDD。残日数算出の基準 |
| 10-40 | 31 | FILLER | X(31) | |

**D: 明細 (1 編成 1 レコード)** (`WS-IN-DTL`)

| 桁 | 長さ | 項目 | PIC | 内容 |
|---|---|---|---|---|
| 1 | 1 | レコード区分 | X(01) | `D` |
| 2-7 | 6 | 編成番号 | X(06) | 左詰め・右空白埋め (例: `U3101 `) |
| 8-15 | 8 | 前回交番検査日 | 9(08) | YYYYMMDD |
| 16-23 | 8 | 前回重要部検査日 | 9(08) | YYYYMMDD |
| 24-31 | 8 | 前回全般検査日 | 9(08) | YYYYMMDD |
| 32-38 | 7 | 重要部検査後 走行 km | 9(07) | ゼロ埋め |
| 39-40 | 2 | FILLER | X(02) | |

**T: トレーラ** (`WS-IN-TRL`)

| 桁 | 長さ | 項目 | PIC | 内容 |
|---|---|---|---|---|
| 1 | 1 | レコード区分 | X(01) | `T` |
| 2-7 | 6 | 件数 | 9(06) | それまでに読んだ D レコード件数と一致すること |
| 8-40 | 33 | FILLER | X(33) | |

例 (`batch/cobol/data/FORMATIONS.DAT`):

```
H20261005
DU3101 2026092520230115201810150338000
...
T000006
```

### 2.2 出力 `INSPDUE.DAT` (レコード長 48 桁, `OUT-REC PIC X(48)`)

各レコードは `MOVE SPACES TO OUT-REC` の後に作業領域を MOVE して WRITE する。

**H: ヘッダ** (`WS-OUT-HDR`) — 入力 H レコードを読んだ時点で 1 件出力

| 桁 | 長さ | 項目 | PIC | 内容 |
|---|---|---|---|---|
| 1 | 1 | レコード区分 | X(01) | `H` |
| 2-9 | 8 | 基準日 | 9(08) | 入力 H の基準日をそのまま |

**D: 明細** (`WS-OUT-DTL`) — 入力 D レコード 1 件につき 1 件出力

| 桁 | 長さ | 項目 | PIC | 内容 |
|---|---|---|---|---|
| 1 | 1 | レコード区分 | X(01) | `D` |
| 2-7 | 6 | 編成番号 | X(06) | 入力の 6 桁をそのまま (前後空白も含めて転記) |
| 8-15 | 8 | 交番検査期限 | 9(08) | YYYYMMDD (§3.1) |
| 16-23 | 8 | 重要部検査期限 | 9(08) | YYYYMMDD (§3.2) |
| 24-31 | 8 | 全般検査期限 | 9(08) | YYYYMMDD (§3.3) |
| 32 | 1 | 次回検査種別 | X(01) | `K` 交番 / `J` 重要部 / `Z` 全般 (§3.5) |
| 33-40 | 8 | 次回検査期限 | 9(08) | YYYYMMDD |
| 41 | 1 | 残日数 符号 | X(01) | `+` (0 以上) / `-` (負) |
| 42-45 | 4 | 残日数 (絶対値) | 9(04) | ゼロ埋め。**下 4 桁のみ** (§3.6) |
| 46 | 1 | 走行 km 超過フラグ | X(01) | `K` = 60 万 km 以上 / 空白 |
| 47 | 1 | 判定 | X(01) | `X` 超過 / `W` 注意 / `N` 正常 (§3.7) |
| 48 | 1 | (未使用) | | 空白。行末空白のため実際には出力されない |

**T: トレーラ** (`WS-OUT-TRL`) — EOF 後に 1 件出力

| 桁 | 長さ | 項目 | PIC | 内容 |
|---|---|---|---|---|
| 1 | 1 | レコード区分 | X(01) | `T` |
| 2-7 | 6 | 件数 | 9(06) | 処理した D レコード件数 |
| 8-13 | 6 | 注意件数 | 9(06) | 判定 `W` の件数 |
| 14-19 | 6 | 超過件数 | 9(06) | 判定 `X` の件数 |

例 (`batch/cobol/expected/INSPDUE.DAT`):

```
H20261005
DU3101 202612242027011520261015Z20261015+0010 W
DU5103 202609262027091220270912K20260926-0009 X
...
T000006000003000001
```

## 3. 業務ルール (CALC-DUE / ADD-YEARS)

検査周期の定数 (`WS-RULE`, L59-65):

| 定数 | 値 | 意味 |
|---|---|---|
| `WS-KOBAN-DAYS` | 90 | 交番検査周期 (日) |
| `WS-JUYOBU-YEARS` | 4 | 重要部検査周期 (年) |
| `WS-ZENPAN-YEARS` | 8 | 全般検査周期 (年) |
| `WS-JUYOBU-KM-LIMIT` | 600000 | 重要部検査 走行 km 上限 (超過判定) |
| `WS-JUYOBU-KM-WARN` | 570000 | 重要部検査 走行 km 注意 |
| `WS-WARN-DAYS` | 14 | 注意判定の残日数 |

### 3.1 交番検査期限 (K)
`交番期限 = 前回交番検査日 + 90 日`
(`DATE-OF-INTEGER(INTEGER-OF-DATE(前回) + 90)`。暦日加算で月末・年跨ぎ・うるう年は暦どおり)

### 3.2 重要部検査期限 (J)
`重要部期限 = 前回重要部検査日 + 4 年` (ADD-YEARS)

走行 km 条件 (60 万 km) は**期限日には反映されない**。期限日はあくまで日付のみで算出し、走行 km は超過フラグと判定 (§3.7) にだけ使われる。つまり「4 年 または 60 万 km の早い方」は、日付側は期限日、km 側は判定 `X` として表現される。

### 3.3 全般検査期限 (Z)
`全般期限 = 前回全般検査日 + 8 年` (ADD-YEARS)

### 3.4 年加算 (ADD-YEARS) と 2/29 補正
```
年   = 元日付 / 10000 (整数部) + 加算年数
月日 = 元日付の 5-8 桁目 (MMDD)
if 月日 = 0229 and 年 がうるう年でない then 月日 = 0228
結果 = 年 * 10000 + 月日
```
- うるう年判定: (4 で割り切れ かつ 100 で割り切れない) または 400 で割り切れる (グレゴリオ暦)。
- 2/29 以外の日付は月日をそのまま使う (日付の妥当性チェックはしない)。
- 例: 2024/02/29 + 4 年 → 2028/02/29 (うるう年なのでそのまま)、2096/02/29 + 4 年 → 2100/02/28 (2100 年は平年)、2000/02/29 + 8 年 → 2008/02/29。

### 3.5 次回検査 (種別・期限)
3 つの期限のうち最も早いものを次回検査とする。同日の場合は **K → J → Z の順で優先** (比較が厳密な `<` のため、先に採用された種別が残る)。
```
次回 = (K, 交番期限)
if 重要部期限 < 次回期限 then 次回 = (J, 重要部期限)
if 全般期限   < 次回期限 then 次回 = (Z, 全般期限)
```

### 3.6 残日数
`残日数 = INTEGER-OF-DATE(次回期限) - INTEGER-OF-DATE(基準日)` (基準日当日が期限なら 0)

- 負なら符号 `-`、0 以上なら `+`。絶対値を 4 桁で出力する。
- 作業領域は `WS-DAYS PIC S9(05)` → `WS-ABS-DAYS PIC 9(05)` → `WS-OD-DAYS PIC 9(04)`。COBOL の MOVE は上位桁を切り捨てるため、**絶対値が 10000 日以上の場合は下 4 桁だけが出力される** (例: 12231 日 → `+2231`。付録 A ケース 06)。判定 (§3.7) は切り捨て前の `WS-DAYS` で行う。

### 3.7 走行 km 超過フラグと判定

走行 km 超過フラグ: `走行 km >= 600000` なら `K`、それ以外は空白。

判定 (上から順に評価し、最初に当てはまったもの):

| 判定 | 条件 | カウンタ |
|---|---|---|
| `X` 超過 | 残日数 < 0 **または** 走行 km >= 600000 | 超過件数 +1 |
| `W` 注意 | 残日数 <= 14 **または** 走行 km >= 570000 | 注意件数 +1 |
| `N` 正常 | 上記以外 | — |

境界値 (付録 A ケース 07 で実測): 残 14 日 = `W`, 残 15 日 = `N`, 残 0 日 = `W`, 残 -1 日 = `X`, 569999 km = `N`, 570000 km = `W`, 599999 km = `W`, 600000 km = `X` + フラグ `K`。

## 4. 戻り値 (RETURN-CODE)

| RC | 意味 | 発生箇所 | 出力ファイルの状態 |
|---|---|---|---|
| 0 | 正常 (超過なし) | 終了時 (L156-160) | 完全 (H/D/T) |
| 4 | 正常終了・超過編成あり (超過件数 > 0) | 終了時 | 完全 (H/D/T) |
| 8 | トレーラ件数不一致 | T レコード読込時 (L178-182) | **途中まで**。それまでの H と D は出力済み、T は出力されない。`STOP RUN` で即終了し、GnuCOBOL が暗黙 CLOSE する |
| 12 | 入力ファイル オープンエラー | 開始時 (L127-132) | **作成されない** (OUT-FILE を OPEN する前に終了) |

- `run.sh` は RC >= 8 を異常として終了コードをそのまま返し、RC 0/4 のときだけゴールデン比較を行う。現行データでは RC=4 (超過 1 件) が正常結果。
- RC=8/12 では最後の `I:` メッセージは表示されない。
- 出力ファイルの OPEN 失敗・READ エラー (EOF 以外) は検査していない。

## 5. コンソール出力

`DISPLAY` で標準出力へ出す。メッセージ中の数値は PIC の桁数でゼロ埋めされる。

| 種別 | 文言 | 出るとき |
|---|---|---|
| `I` | `URINS01 I: 基準日=YYYYMMDD 編成=nnnnnn 注意=nnnnnn 超過=nnnnnn` | 正常終了時 (RC 0/4) に 1 回 |
| `W` | `URINS01 W: 不明なレコード区分 x` (x = 先頭 1 文字) | 先頭が H/D/T 以外のレコードごと。処理は継続 |
| `E` | `URINS01 E: FORMATIONS.DAT OPEN ERROR ss` (ss = FILE STATUS, 例: ファイル無しは `35`) | RC=12 |
| `E` | `URINS01 E: トレーラ件数不一致` | RC=8 |

実例: `URINS01 I: 基準日=20261005 編成=000006 注意=000003 超過=000001`

補足:
- **空行**も「不明なレコード区分」扱い (先頭が空白) で `URINS01 W: 不明なレコード区分 ` (末尾に空白 1 文字) が出る。
- RC=8 の場合、GnuCOBOL ランタイムが標準エラーに `libcob: warning: implicit CLOSE of OUT-FILE ('INSPDUE.DAT')` / `... FORM-FILE ('FORMATIONS.DAT')` を出す (ランタイム依存のメッセージで、業務仕様ではない)。
- 基準日の `I:` 表示は、最後に読んだ H レコードの日付。

## 6. 入力異常時の挙動 (COBOL の実挙動)

COBOL 版は入力の妥当性チェックをほとんど行わない。以下は GnuCOBOL 3.1.2 での実測で、業務的に意図された仕様とは限らない。Java 化で再現するか、エラーにするかは別途決定が必要 (§7 の「要判断」)。

| 入力 | COBOL の挙動 |
|---|---|
| H レコードが複数 | H ごとに出力 H を 1 件書き、以降の D は新しい基準日で計算。件数カウンタはリセットされない (ケース 05) |
| H レコードが無い | 基準日 = 0 として計算 (残日数は不定値)。出力に H が無い。`I:` は `基準日=00000000` (ケース 11) |
| T の後ろに D | 通常どおり処理・出力・カウント (ケース 04) |
| T が無い | 件数検証なしで正常終了 (ケース 03) |
| 存在しない日付 (例 20230230) | `INTEGER-OF-DATE` が 0 を返し、期限が 1601 年等の値になる。通常は `X` 判定 (ケース 12) |
| 走行 km に数字以外 | エラーにならず処理継続 (値は不定) (ケース 13) |
| 40 桁未満の短い D | 空白埋めされた値で計算され、不定な期限が出る (ケース 14) |
| 編成番号に前空白 (例 ` U31  `) | 6 桁をそのまま転記 (ケース 15) |

## 7. 既存 Java 実装との差分一覧

対象: `src/main/java/jp/usagi/railway/service/InspectionService.java`

- `calculate(...)` (static): 1 編成分の期限・次回・残日数・判定を算出
- `processFormationsFile(List<String> lines, int warnDays)` (static): FORMATIONS.DAT の行リストを受けて INSPDUE.DAT の行リストを返す
- `formatDueFile` / `formatRecord`: 出力行の組み立て
- `formationsFile()` / `inspectionDueFile()`: DB から FORMATIONS.DAT / INSPDUE.DAT 相当を作る (`BatchApiController` の `/api/batch/formations-file`, `/api/batch/inspection-due` と画面「検査期限」が使用)
- 現状の契約テスト: `CobolParityTest` (golden の正常系 6 編成のみ 1 バイト一致を確認)

### 7.1 一致している点 (付録 A で実測)

| 項目 | 内容 |
|---|---|
| 交番 +90 日 | `LocalDate.plusDays(90)` |
| 重要部 +4 年 / 全般 +8 年と 2/29 → 2/28 | Joda `plusYears` は平年の 2/29 を 2/28 に丸める。ADD-YEARS と同結果 (2100 年も 2/28) |
| 次回検査 K→J→Z 優先 | `isBefore` (厳密な `<`) で同じ順序 |
| 残日数 | `Days.daysBetween(base, nextDue)` |
| 判定 X/W/N と境界値、km フラグ | 同じ条件・同じ評価順 |
| 出力 H/D/T の形式 | 正常系・境界値・同日・うるう年のケースでバイト一致 |
| 空行 | 出力上は同じ (COBOL は警告を出して読み飛ばし、Java は黙って読み飛ばし) |
| T の後ろの D / T 無し | 出力は同じ (Java は T をもともと見ていない) |

### 7.2 ギャップ (Java 化で埋めるもの)

| # | 項目 | COBOL URINS01 | Java 現状 | Java 化での対応 |
|---|---|---|---|---|
| G1 | ファイル I/O | `FORMATIONS.DAT` を読み `INSPDUE.DAT` に書く (カレントディレクトリ, LINE SEQUENTIAL) | `List<String>` を受け渡すだけ。ファイルを読み書きする入口が無い | 単独 main でファイル入出力を実装 (LF 改行, 末尾空白なし) |
| G2 | 起動方式 | 実行ファイル `urins01` (JP1 ジョブ / `run.sh` から起動) | Spring サービスの static メソッド | 単独 main (既存 `InspectionService` の static メソッドを再利用) |
| G3 | 戻り値 | 0 / 4 / 8 / 12 (§4) | 無し (正常時は値を返し、異常時は例外) | プロセス終了コードとして 0/4/8/12 を返す |
| G4 | トレーラ件数検証 | T の件数 ≠ それまでの D 件数 → `E:` 表示, RC=8, その時点で終了 | T レコードを無視 (`default: break`) | 件数検証を追加。不一致時は RC=8 |
| G5 | RC=8 時の出力 | H と D は出力済み、T は出力しない (途中までのファイルが残る) | 常に H/D/T をすべて返す (ケース 01) | COBOL と同じく途中まで残すかを決める (要判断。下記) |
| G6 | 入力オープンエラー | `E: FORMATIONS.DAT OPEN ERROR 35`, RC=12, 出力ファイルを作らない | 入口が無いため該当なし | 入力が開けない場合 RC=12、出力ファイルを作らない |
| G7 | 不明レコード区分 | `W: 不明なレコード区分 x` を表示し継続 (空行含む) | 空行・不明区分とも黙って無視 | 警告を表示 (文言は §5 と同一) |
| G8 | 終了メッセージ | `I: 基準日=... 編成=... 注意=... 超過=...` | 無し | 同一文言で表示 |
| G9 | H レコード複数 | H ごとに出力 H を書き、その後の D は新基準日で計算 | 最後の基準日で H を 1 件だけ出力。ただし各 D は読んだ時点の基準日で計算 (ケース 05) | COBOL に合わせる (要判断: 業務上 H 複数はありえないならエラー扱いでもよい) |
| G10 | H レコード無し | 基準日 0 で処理継続・H 無しで出力, RC 0 | `IllegalArgumentException` で異常終了 | 要判断 (COBOL の不定値は再現困難。エラー終了を推奨) |
| G11 | 残日数 10000 日以上 | 下 4 桁に切り捨て (`+2231`)。行長 47 桁のまま | `%04d` のため 5 桁で出力 (`+12231`)、行長が 48 桁になる (ケース 06) | 下 4 桁に切り捨てる (`% 10000`) か、範囲外をエラーにするか要判断。業務上は前回検査日が 27 年以上前/先の異常データのみで発生 |
| G12 | 編成番号の前後空白 | 6 桁をそのまま転記 | `trim()` 後 `%-6s` で左詰め。前空白があると出力が変わる (ケース 15) | 6 桁をそのまま転記 (`substring(1, 7)` を trim しない) |
| G13 | 存在しない日付・数字以外の km・短いレコード | エラーにならず不定値で出力 (§6) | 例外 (`IllegalFieldValueException` / `NumberFormatException` / `StringIndexOutOfBoundsException`) で異常終了 | 要判断。COBOL の不定値はバイト一致させる意味が薄く、エラー終了 (RC を別途定義) を推奨 |
| G14 | 注意日数 | 定数 14 | 引数 `warnDays`。Web 側は `urms.inspection.warn-days` (既定 14) | バッチは 14 固定 (または同プロパティ既定値) |
| G15 | 文字コード・改行 | ASCII, LF | `BatchApiController` は UTF-8, LF で行連結 | LF、末尾改行あり (COBOL 出力の最終行も LF で終わる) |

「要判断」(G5, G9, G10, G11, G13) は COBOL の実挙動を記録したもの。Java 化の方針決定 (起動方式の設計) で扱いを決め、境界値ゴールデンデータ作成時にケースとして固定する。

決定 (境界値ゴールデンデータ作成時, 付録 B): G5・G9・G11 は COBOL の出力をゴールデンとして固定する。G10・G13 は COBOL の不定値を正解にせず、Java は `URINS01 E:` で始まるメッセージを出して RC=12 でエラー終了とする (入力と期待 RC・メッセージ接頭辞だけを固定)。G10 の「H 無し」は D の有無にかかわらず適用する (0 バイトの入力も含む)。 Java 版での実装方針は §10 (Java 設計) を参照。

## 8. 実行環境メモ

- `run.sh [入力] [期待出力 | -]` は `cobc -x -o work/urins01 URINS01.cbl` でビルドし、`work/` に入力をコピーして実行。期待出力 (既定 `expected/INSPDUE.DAT`, `-` で比較なし) と `diff` で比較する。入力ファイルが存在しない場合はコピーせずに実行し、COBOL 自身が RC=12 を返す。実行結果は `work/RC.TXT` / `work/SYSOUT.TXT` / `work/SYSERR.TXT` にも保存される。
- `batch/java/run.sh [FORMATIONS.DAT へのパス] [期待する INSPDUE.DAT のパス | -]` は同じインターフェースで Java 版を実行し、`batch/java/work/` に結果を保存する。`URMS_WAR` 未指定時は最新の WAR を使い、無い場合または `src/main` / `pom.xml` が WAR より新しい場合は Maven で自動ビルドする。`URMS_WAR` 指定時はビルドを省略し、Java は `URMS_JAVA`、未指定なら `JAVA_HOME/bin/java` (JAVA_HOME も未指定なら `java`) を使う。起動は `batch/java/urins01` ラッパー経由。
- `gen-boundary.sh` は付録 B の境界値ゴールデンデータを `run.sh` 経由で再生成する。
- バージョン表記の不一致: Jenkinsfile のコメントは GnuCOBOL 2.2、`run.sh` のコメントは 3.x、本書の実測環境は GnuCOBOL 3.1.2.0。
- Jenkinsfile: `Batch Golden Test` ステージで `batch/cobol/run.sh` を実行、`work/urins01` をアーカイブし、ステージングの `jp1adm@urms-bat-stg01:/opt/urms/bin/` に scp。
- 入出力ファイル名はプログラム内に固定 (`ASSIGN TO "FORMATIONS.DAT"` / `"INSPDUE.DAT"`)。カレントディレクトリからの相対パス。

## 9. 影響範囲 (呼び出し元・依存の棚卸し)

COBOL バッチ URINS01 に依存している箇所の一覧。移行方針 (COBOL URINS01 のみを対象、C URPWD01 は範囲外 / 既存 `InspectionService` を再利用する単独 main / COBOL 出力をゴールデンとしたバイト一致 + CI / COBOL ソースは最終的に削除) を前提に、各箇所を次の 3 つに分類する。

- **変更**: Java 化に合わせて書き換える
- **維持**: 変更しない (移行後もそのまま使う、またはバイト一致で影響が無い)
- **削除**: COBOL 削除時に取り除く

「担当」は移行計画のステップ (s2.1 起動方式設計 / s3.1 `Urins01Batch` 実装 / s3.2 実行スクリプト / s4.1 境界値ゴールデン / s4.2 パリティテスト / s5.1 Jenkinsfile 切替 / s5.2 COBOL 削除 / s5.3 ドキュメント・画面表記)。行番号は統合ブランチ `migration/cobol-to-java` (a9cf0f8) 時点のもの。

### 9.1 COBOL 本体・実行スクリプト・データ

| # | 箇所 | 依存内容 | 分類 | 担当 | 備考 |
|---|---|---|---|---|---|
| A1 | `batch/cobol/URINS01.cbl` | COBOL ソース本体 | 削除 | s5.2 | s4.1 (境界値ゴールデン作成) とパリティ確認が終わるまで残す |
| A2 | `batch/cobol/run.sh` | `cobc -x` でビルドして実行、RC >= 8 で異常終了、RC 0/4 なら `expected/INSPDUE.DAT` と diff | 削除 | s5.2 | Java 版実行スクリプト (s3.2) が置き換える。RC 判定 (>= 8 異常) と `GOLDEN OK` / `GOLDEN MISMATCH` の比較手順は引き継ぐ |
| A3 | `batch/cobol/data/FORMATIONS.DAT` | URINS01 の入力 fixture | 維持 | — | 内容は変更しない。`batch/cobol/` を削除する場合は配置先の移動のみ (s2.1 / s5.2 で決定) |
| A4 | `batch/cobol/expected/INSPDUE.DAT` | URINS01 の出力 (正解) | 維持 | — | Java 版のバイト一致の正解。A3 と同じく配置先の移動のみありうる |
| A5 | `batch/cobol/work/` (生成物 `urins01`, 入出力のコピー) | `run.sh` の作業ディレクトリ。git 管理外 | 削除 | s5.2 | Java 版の作業ディレクトリに置き換わる |

### 9.2 CI (Jenkinsfile)・配布

| # | 箇所 | 依存内容 | 分類 | 担当 | 備考 |
|---|---|---|---|---|---|
| B1 | `Jenkinsfile` L4 `agent { label 'rhel7-jdk8-cobol' }` | GnuCOBOL 入りの CI エージェントを要求 | 変更 | s5.1 | COBOL 削除後は GnuCOBOL 不要。ただし C URPWD01 (範囲外) のため gcc は引き続き必要。ラベルの付け替えは Jenkins 側 (リポジトリ外) の設定と合わせる |
| B2 | `Jenkinsfile` L55 コメント `gcc 4.8 / GnuCOBOL 2.2 は CI エージェントにプリインストール` | GnuCOBOL 前提の記述 | 変更 | s5.1 | GnuCOBOL を外す。実測環境は 3.1.2 (§8) |
| B3 | `Jenkinsfile` L58 `stage('COBOL URINS01') { steps { sh 'cd batch/cobol && ./run.sh' } }` | `Batch Golden Test` (parallel) の COBOL ステージ | 変更 | s5.1 | Java 版実行スクリプト (s3.2) によるゴールデン比較に置き換える。同じ parallel 内の `C URPWD01` ステージは維持 |
| B4 | `Jenkinsfile` L65 `archiveArtifacts ... batch/cobol/work/urins01` | COBOL 実行ファイルの成果物保存 | 変更 | s5.1 | Java 版の成果物 (jar / 起動スクリプト。形式は s2.1 で決定) に置き換える。`target/*.war`, `batch/c/work/urpwd01` は維持 |
| B5 | `Jenkinsfile` L84 `scp ... batch/cobol/work/urins01 jp1adm@urms-bat-stg01:/opt/urms/bin/` | ステージングのバッチサーバへ COBOL 実行ファイルを配布 (`release/*` のみ) | 変更 | s5.1 | Java 版の成果物を配布する。`batch/c/work/urpwd01` の配布は維持 |
| B6 | `Jenkinsfile` L39 `Unit Test` (`-Dtest=*Test`) / L50 `Integration Test` (`-Dtest=*IT`) | `CobolParityTest` は `*Test` に一致するため `Unit Test` で実行される | 維持 | s4.2 | 新しいパリティテストもクラス名を `*Test` (または `*IT`) にしないと CI で実行されない点に注意 |

### 9.3 Java 本体・画面

| # | 箇所 | 依存内容 | 分類 | 担当 | 備考 |
|---|---|---|---|---|---|
| C1 | `src/main/java/jp/usagi/railway/service/InspectionService.java` (`calculate` / `processFormationsFile` / `formatDueFile` / `formatRecord`) | URINS01 と同一ロジック。Java 版バッチが再利用する本体 | 変更 | s3.1 | §7.2 のギャップ (G4〜G13) への対応。`formatRecord` / `formatDueFile` は画面・API (`inspectionDueFile()`) と共用のため、G11 (残日数 4 桁切り捨て) 等の変更は画面・API の出力にも及ぶ。シードデータ (残日数はすべて 4 桁以内) では差は出ない |
| C2 | `InspectionService.java` L20 Javadoc `COBOL バッチ URINS01 (batch/cobol/URINS01.cbl) と同一ロジック.` / L67 コメント `URINS01 ADD-YEARS と同じ` | COBOL ソースのパスを参照 | 変更 | s5.2 / s5.3 | COBOL 削除後は存在しないパスになる。仕様書 `docs/batch/URINS01.md` への参照に差し替える |
| C3 | `src/main/java/jp/usagi/railway/service/InspectionDue.java` L8 Javadoc `URINS01 出力 D レコード相当` | プログラム ID の参照のみ | 維持 | — | Java 版もプログラム ID `URINS01` を引き継ぐため記述は有効 |
| C4 | `src/main/java/jp/usagi/railway/api/BatchApiController.java` L44-47 `GET /api/batch/formations-file` (Javadoc `URINS01 入力 FORMATIONS.DAT`) | JP1 ジョブ (L18 Javadoc) が URINS01 の入力を DB から取得する口 | 維持 | — | Java 版バッチも `FORMATIONS.DAT` を入力とするため、そのまま使う |
| C5 | `BatchApiController.java` L50-53 `GET /api/batch/inspection-due` (Javadoc `INSPDUE.DAT 相当`) | DB から INSPDUE.DAT 形式を返す | 維持 | — | C1 の `formatRecord` 変更の影響のみ受ける |
| C6 | `src/main/java/jp/usagi/railway/config/SecurityConfig.java` L34 `/api/batch/**` は ADMIN のみ | C4 / C5 の認可 | 維持 | — | |
| C7 | `src/main/webapp/WEB-INF/jsp/rolling/inspections.jsp` L24 `COBOL バッチ URINS01 と同一形式 (INSPDUE.DAT)` | 画面表記 | 変更 | s5.3 | 例: 「検査期限バッチ URINS01 と同一形式」。`fileLines` は `inspectionDueFile()` (C1) |
| C8 | `docs/images/inspections.png` | 上記 C7 の表記を含む画面のスクリーンショット (README から参照)。画像内の文字のため grep では検出されない | 変更 | s5.3 | C7 の変更後に撮り直す |
| C9 | `RollingStockController` (`/rolling/inspections`, 編成一覧・詳細), `DashboardController` (検査期限 注意・超過), `RollingStockApiController` / `FormationDto` (`GET /api/formations`), `formation.jsp`, `dashboard.jsp` | `InspectionService.calculate` / `listAll` / `inspectionDueFile` を使う間接依存。COBOL には依存しない | 維持 | — | s3.1 で `calculate` (static / インスタンス) のシグネチャと結果を変えない限り影響なし |
| C10 | `src/main/resources/application.properties` L74 `urms.inspection.warn-days=14` | Web 側の注意日数 (COBOL は定数 14) | 維持 | s2.1 | G14。バッチ側で 14 固定にするか同プロパティを読むかは s2.1 で決定。値は変えない |

### 9.4 テスト・ゴールデンファイル

| # | 箇所 | 依存内容 | 分類 | 担当 | 備考 |
|---|---|---|---|---|---|
| D1 | `src/test/java/jp/usagi/railway/service/CobolParityTest.java` | golden の正常系 6 編成で `processFormationsFile` と COBOL 出力の 1 バイト一致を検証 | 維持 | s4.2 | 契約テストとして残す。Javadoc の `batch/cobol/URINS01.cbl` 参照は C2 と同様に s5.2 で差し替え。Java 版バッチのファイル入出力・RC のパリティテストは s4.2 で追加 |
| D2 | `src/test/resources/golden/FORMATIONS.DAT` | A3 のコピー | 維持 | — | `cmp` で A3 と同一内容を確認済み |
| D3 | `src/test/resources/golden/INSPDUE_COBOL.DAT` | A4 のコピー (COBOL 出力) | 維持 | — | `cmp` で A4 と同一内容を確認済み。COBOL 削除後は COBOL 出力の唯一の記録になるため変更しない |
| D4 | `src/test/java/jp/usagi/railway/service/GoldenFiles.java` | `/golden/` を読むテスト補助 | 維持 | — | s4.1 / s4.2 の境界値ゴールデンも同じ口で読める |
| D5 | `src/test/java/jp/usagi/railway/service/InspectionServiceTest.java` | `calculate` の単体テスト (周期・判定・km・うるう日) | 維持 | — | COBOL には依存しない |
| D6 | `src/test/java/jp/usagi/railway/api/UrmsApiIT.java` `batchFilesRequireAdmin` | `/api/batch/inspection-due` が `H20261005\nDU3101 ` で始まること | 維持 | — | C5 |

### 9.5 スクリプト・ドキュメント・設定

| # | 箇所 | 依存内容 | 分類 | 担当 | 備考 |
|---|---|---|---|---|---|
| E1 | `demo/reset.sh` L34 `rm -rf batch/c/work batch/cobol/work target` | COBOL の作業ディレクトリを削除 | 変更 | s5.2 | Java 版の作業ディレクトリに合わせる (`batch/c/work`, `target` は維持)。リセット先のタグ `demo/0-legacy` は COBOL を含む状態のまま維持 |
| E2 | `.gitignore` L17-18 `# COBOL batch work dir` / `batch/cobol/work/` | A5 の除外 | 変更 | s5.2 | Java 版の作業ディレクトリに置き換え。`batch/c/work/` は維持 |
| E3 | `README.md` L3 (夜間バッチは C と COBOL), L5 (GnuCOBOL の夜間バッチ), L15 (検査期限 (COBOL バッチと同一形式)), L35 (`URINS01` と同一形式の `INSPDUE.DAT`), L41 (GnuCOBOL `cobc` が必要), L72 (`batch/cobol/run.sh`), L78 (`CobolParityTest` の説明), L86 (golden は `batch/*/data`・`expected` のコピー), L104 (`batch/cobol/URINS01.cbl` 1997 年製), L116 (移行テーマ「3. COBOL バッチ → Java」) | COBOL 前提の記述 | 変更 | s5.3 | L37 (`/api/batch/formations-file`, `/inspection-due`)・L92 (Jenkinsfile の「バッチサーバへ配布」) は維持 |
| E4 | `.agents/skills/urms-testing/SKILL.md` L3 (C/COBOL batches), L12 (`batch/cobol/run.sh` (cobc)), L34 (C / COBOL batch と同一形式) | テスト手順の COBOL 前提 | 変更 | s5.3 | RC=4 が正常という記述は Java 版でも同じ (RC 互換のため) |
| E5 | `docs/DEMO.md` L5, L20, L30, L44, L49-54, L87-99, L124, L134 | デモ台本。`demo/0-legacy` (COBOL あり) を出発点に「C / COBOL を Java 化する」こと自体が題材 | 維持 | — | 台本は移行前の状態を前提にしているため書き換えない |
| E6 | `docs/batch/URINS01.md` (本書) §8 の `run.sh` / Jenkinsfile の記述 | COBOL 版の記録 | 維持 | — | COBOL 版の仕様 (契約) として残す。Java 版の起動方法は s2.1 / s3.2 で追記 |

### 9.6 リポジトリ外・他ブランチ (grep の対象外)

| # | 箇所 | 依存内容 | 分類 | 担当 | 備考 |
|---|---|---|---|---|---|
| F1 | JP1 ジョブ定義 (運用管理サーバ) | `/api/batch/formations-file` で入力を取得し (`BatchApiController` L18)、バッチサーバ `/opt/urms/bin/urins01` を起動 (B5) | 変更 | s2.1 / s5.1 | 起動コマンドを Java 版に変える。RC 0/4/8/12 (§4) の判定はそのまま使えるよう、Java 版は同じ RC を返す (G3)。定義自体はリポジトリに無いため、運用部門への依頼事項 |
| F2 | 検修計画システム (`INSPDUE.DAT` の連携先, `URINS01.cbl` L12) | 出力ファイルの受け手 | 維持 | — | バイト一致で移行するため影響なし |
| F3 | バッチサーバ `urms-bat-stg01` (と本番) の実行環境 | 現状は GnuCOBOL ランタイムで `urins01` を実行 | 変更 | s2.1 / s5.1 | Java 版の実行には JRE 8 が必要。サーバに JRE があるかはリポジトリから判断できない (要確認) |
| F4 | Jenkins エージェント `rhel7-jdk8-cobol` | GnuCOBOL 2.2 をプリインストール (B1, B2) | 変更 | s5.1 | COBOL 削除後は不要。gcc (C URPWD01) と JDK 8 は引き続き必要 |
| F5 | Devin 環境 (blueprint): `gnucobol3` パッケージ (`/usr/bin/cobc`, 3.1.2.0) | `batch/cobol/run.sh` の実行・境界値ゴールデンの作成 | 維持 | s4.1 | s4.1 / s4.2 でゴールデン作成・照合に必要。COBOL 削除 (s5.2) 後に外すかは任意 |
| F6 | ブランチ `customer/kame-dentetsu`, `customer/kitsune-kotsu` | 統合ブランチと同じ COBOL 関連ファイルを持つ (`batch/cobol/`, `InspectionService.java`, `Jenkinsfile` の差分なしを確認) | 維持 | — | 本移行 (`migration/cobol-to-java` → `main`) の範囲外。納入先別の版へは別途取り込みが必要 |
| F7 | タグ `demo/0-legacy` | COBOL を含む初期状態。`demo/reset.sh` の既定のリセット先 | 維持 | — | デモの出発点のため変更しない |

### 9.7 範囲外 (C URPWD01) との共用箇所

次の箇所は C URPWD01 と COBOL URINS01 が同じ行・同じブロックを共有している。COBOL 側を変更するときは C 側の記述を残すこと。

- `Jenkinsfile` L54-60 `Batch Golden Test` の parallel (`C URPWD01` ステージは維持)、L65 `archiveArtifacts`、L84 `scp`
- `.gitignore` L17-19 (`batch/c/work/` は維持)、`demo/reset.sh` L34
- `README.md` L3, L5, L41, L86 / `SKILL.md` L3, L12, L34 (C と COBOL を並べて書いている)

### 9.8 漏れ確認

統合ブランチで次の grep を実行し、ヒットしたファイル 14 件 (本書を除くと 13 件) がすべて上表に含まれることを確認した (`.git`, `target`, `work` は除外)。

```
$ grep -rIil "cobol\|urins01\|inspdue" --exclude-dir=.git --exclude-dir=target --exclude-dir=work .
./.agents/skills/urms-testing/SKILL.md      → E4
./.gitignore                                → E2
./Jenkinsfile                               → B1-B5
./README.md                                 → E3
./batch/cobol/URINS01.cbl                   → A1
./batch/cobol/run.sh                        → A2
./demo/reset.sh                             → E1
./docs/DEMO.md                              → E5
./docs/batch/URINS01.md                     → E6 (本書)
./src/main/java/jp/usagi/railway/api/BatchApiController.java          → C4, C5
./src/main/java/jp/usagi/railway/service/InspectionDue.java           → C3
./src/main/java/jp/usagi/railway/service/InspectionService.java       → C1, C2
./src/main/webapp/WEB-INF/jsp/rolling/inspections.jsp                 → C7
./src/test/java/jp/usagi/railway/service/CobolParityTest.java         → D1
```

上記 grep にかからない依存は、ファイル名・API・メソッド名 (`FORMATIONS.DAT`, `formations-file`, `inspection-due`, `processFormationsFile`, `inspectionDueFile`, `InspectionService`) の grep と、画像・リポジトリ外の参照の確認で追加した (A3-A5, B6, C6, C8-C10, D2-D6, F1-F7)。

---

## 10. Java 設計 (起動方式・クラス構成)

本章は Java 版 URINS01 の設計 (実装は s3.1)。方針は「既存 `InspectionService` の static メソッドを再利用する単独 main (RC を返す CLI)」(決定 `plain-main`)。Spring Batch は使わず、Spring コンテキストも起動しない (純粋なファイル入出力のため)。受け入れ基準は付録 B の全 19 ケース。

### 10.1 クラス構成

| クラス / メソッド | 区分 | 役割 |
|---|---|---|
| `jp.usagi.railway.batch.Urins01Batch` | 新規 | バッチ本体。`main` と、テストから呼べる `run` を持つ。状態はローカル変数のみ (static な可変状態を持たない) |
| `Urins01Batch.main(String[] args)` | 新規 | `int rc = run(args, console)` → `console.flush()` → `System.exit(rc)`。`Throwable` をすべて捕捉して RC=12 にする (§10.5) |
| `Urins01Batch.run(String[] args, PrintStream console)` (package-private, static) | 新規 | 引数解釈 → 入力読込 → レコード処理 → 出力書込 → サマリ表示。RC (0/4/8/12) を返し、`System.exit` は呼ばない。単体テスト・パリティテストの入口 |
| `InspectionService.calculate(...)` (static) | 再利用・変更なし | 1 編成分の期限・次回・残日数・判定。`warnDays` は 14 を渡す (G14) |
| `InspectionService.formatRecord(InspectionDue)` (static) | 再利用・変更なし | D レコードの組み立て。G11 の 4 桁切り捨てはバッチ側で後処理する (§10.4) |
| `InspectionService.formatDueFile(LocalDate, List)` (static) | 再利用 (H/T 書式の抽出のみ) | 一括整形 (H 1 件 + D + T) のため、H 複数 (G9) と途中打ち切り (G5) を満たせずバッチからは直接呼ばない。s3.1 で H/T の書式を `formatHeader(LocalDate)` / `formatTrailer(int count, int warn, int over)` (public static) として抽出し、`formatDueFile` もそれを呼ぶ形にする。出力は 1 バイトも変えない (純粋なリファクタリング) |
| `InspectionService.processFormationsFile(...)` (static) | 変更なし | `CobolParityTest` の契約テスト対象として現状のまま残す。バッチからは呼ばない (RC・警告・逐次出力を持たないため。trim も残る) |

二重実装しないもの: 期限・次回・残日数・判定の計算 (`calculate`)、D レコード書式 (`formatRecord`)、H/T 書式 (`formatHeader` / `formatTrailer`)、W/X の件数の数え方 (`formatDueFile` と同じ規則)。
バッチ側に持つもの: ファイル入出力 (LINE SEQUENTIAL 相当)、レコード区分の振り分け、入力項目の検証、トレーラ件数検証、件数カウンタ、コンソールメッセージ、RC 決定。

### 10.2 起動コマンド

実行可能 WAR (`mvn package` の成果物。`spring-boot-maven-plugin` が作る `WarLauncher` 形式) に含まれる Spring Boot 1.5.22 の `PropertiesLauncher` で、`Urins01Batch` を main クラスとして起動する。

```sh
java -Dloader.main=jp.usagi.railway.batch.Urins01Batch \
     -Dloader.path=WEB-INF/classes,WEB-INF/lib \
     -cp /opt/urms/lib/usagi-railway.war \
     org.springframework.boot.loader.PropertiesLauncher [入力ファイル] [出力ファイル]
```

- `-Dloader.path=WEB-INF/classes,WEB-INF/lib` は**必須**。チケットの当初案 (`-cp target/usagi-railway*.war -Dloader.main=... PropertiesLauncher`) だけでは、`PropertiesLauncher` が WAR 内の `WEB-INF/classes` / `WEB-INF/lib` をクラスパスに入れず `ClassNotFoundException` (RC=1) になる (§10.9 の検証 a)。
- `WEB-INF/lib-provided/` (組込み Tomcat・Jasper) は `loader.path` に入れない。バッチのクラスパスに Web コンテナが載らず、Web アプリも起動しない。
- `java -jar usagi-railway.war` は `-Dloader.main` を付けても Manifest の `Main-Class: WarLauncher` / `Start-Class: UsagiRailwayApplication` が使われ、Web アプリ (Tomcat :8080) が起動してしまう (検証 f)。**`-jar` は使わない**。
- `loader.path` の相対パスは WAR 内のエントリとして解決されるため、カレントディレクトリに依存しない。WAR を絶対パスで指定し、別ディレクトリで起動しても動く (検証 e)。カレントディレクトリは入出力ファイルの既定パスにだけ使う (COBOL と同じ)。
- 起動〜終了は約 0.19 秒 (検証 g。Spring コンテキストを起動しないため)。
- WAR は Web と同一の成果物を使う。バッチ専用 JAR を別ビルドしないので、Web とバッチで `InspectionService` の版がずれない。

### 10.3 引数・入出力ファイル

| 引数 | 意味 | 既定 |
|---|---|---|
| 第 1 引数 | 入力ファイルパス | `FORMATIONS.DAT` (カレントディレクトリ) |
| 第 2 引数 | 出力ファイルパス | `INSPDUE.DAT` (カレントディレクトリ) |
| 3 個以上 | `URINS01 E: 引数不正` を表示し RC=12 (入出力ファイルには触れない) | — |

引数なしの起動が COBOL (`ASSIGN TO "FORMATIONS.DAT"` / `"INSPDUE.DAT"`) と同じ動作になる。JP1 からは引数なしで呼ぶ (§10.7)。

**入力 (LINE SEQUENTIAL 相当)**

- 全体を `Files.readAllBytes` で読み、ISO-8859-1 で文字列化する (1 バイト = 1 文字。桁位置がバイト位置と一致し、ASCII 以外のバイトも変換せずに転記できる)。
- LF で区切って 1 行 = 1 レコード。行末の CR は 1 文字除去する (CRLF 入力も COBOL と同じく受け付ける)。最終行に LF が無くても 1 レコードとして扱う。最後の LF の後ろの空文字列はレコードにしない。
- 空行は「区分 = 空白」のレコードとして扱い、`W:` 警告の対象 (G7)。
- 41 桁目以降は無視する (`PIC X(40)` と同じ)。不足分は空白とみなす。ただし D の長さ不足は G13 でエラーにする (§10.4)。
- 入力ファイルが開けない場合は出力ファイルを作らず (既存ファイルにも触れず) RC=12 (G6)。

**出力**

- ISO-8859-1 で書き、各レコードの末尾に LF を付ける (最終レコードも LF で終わる。CR は付けない)。内容は ASCII のため UTF-8 / ASCII とバイト一致する (G15)。
- 書き出し前に末尾空白を除去する (LINE SEQUENTIAL の `WRITE` と同じ)。H/D/T はいずれも非空白文字で終わるため、現行書式では実質的に変化しない。
- 出力は処理順に逐次書く (`BufferedWriter`、try-with-resources で必ず close)。RC=8 では T を書かずに close し、それまでの H/D を残す (G5)。

**コンソール**

- `URINS01 I:` / `W:` / `E:` はすべて標準出力に書く (COBOL の `DISPLAY` と同じ)。標準エラーには予期しない例外のスタックトレースだけを出す。
- 標準出力は `new PrintStream(new FileOutputStream(FileDescriptor.out), true, "UTF-8")` で **UTF-8 を明示**する。`System.out` のままでは `LANG=C` の環境で `file.encoding=ANSI_X3.4-1968` になり日本語が `???` に化ける (検証 d)。境界値データの `STDOUT.txt` は UTF-8。
- 行区切りは LF 固定 (`println` は `line.separator` に依存するため使わず `print(msg + "\n")`)。

### 10.4 レコード処理と G1〜G15 の扱い

処理は入力順に 1 レコードずつ行い、最初に発生した終了事由で止める (COBOL の `STOP RUN` と同じく、その後ろのレコードは読まない)。

| 区分 (1 桁目) | 処理 |
|---|---|
| `H` | 2〜9 桁目を基準日として検証 (数字 8 桁かつ暦上有効な日付。不正なら G13 エラー)。基準日を更新し、出力 H (`formatHeader`) を書く。件数カウンタはリセットしない (G9) |
| `D` | H をまだ読んでいなければ G10 エラー。長さ 38 桁未満なら G13 エラー。3 つの前回検査日 (8〜15, 16〜23, 24〜31 桁) を日付検証、走行 km (32〜38 桁) を数字 7 桁で検証 (不正なら G13 エラー)。`calculate(編成番号 6 桁そのまま, ..., 現在の基準日, 14)` → D 出力 → 件数・注意・超過をカウント |
| `T` | 2〜7 桁目がそれまでの D 件数 (6 桁ゼロ埋め) と一致しなければ `URINS01 E: トレーラ件数不一致` → T を書かずに RC=8 (G4・G5)。件数欄が数字でない・空白の場合も不一致 (COBOL 実測: D 0 件で `T` のみのレコードも RC=8)。一致すれば何も書かずに継続 (T の後ろの D も処理する。ケース 10) |
| その他 (空行・小文字・空白始まりを含む) | `URINS01 W: 不明なレコード区分 x` (x = 1 桁目。空行は空白) を表示して継続 (G7) |

EOF まで到達したら: H を 1 件も読んでいなければ G10 エラー。読んでいれば T (`formatTrailer(件数, 注意, 超過)`) を書いて close し、`URINS01 I:` を表示、超過 > 0 なら RC=4、それ以外 RC=0。

日付の検証は「数字 8 桁 (`[0-9]{8}`) かつ Joda `LocalDate` として有効 (年 1601〜9999。COBOL `INTEGER-OF-DATE` の有効範囲)」。Joda で解析する前に桁数と文字種 (数字のみ) を確認し、空白や符号を含む値も不正とする。

| # | 対応 (Java 版) | 実装場所 | 確認ケース |
|---|---|---|---|
| G1 | ファイル入出力 (§10.3) | バッチ | 全ケース |
| G2 | 単独 main を `PropertiesLauncher` で起動 (§10.2) | バッチ・ラッパー | 検証 b〜g |
| G3 | `System.exit(0/4/8/12)` (§10.5) | バッチ | 全ケースの RC |
| G4 | T の件数検証、不一致で RC=8 | バッチ | 07 |
| G5 | RC=8 時は H と処理済み D を残し T を書かない。T 後の D は処理しない (COBOL どおり) | バッチ | 07 |
| G6 | 入力が開けなければ `URINS01 E: FORMATIONS.DAT OPEN ERROR 35`、出力を作らず RC=12 | バッチ | 12 |
| G7 | 不明区分ごとに `W:` 警告 (§5 と同一文言、空行含む) | バッチ | 08 |
| G8 | 正常終了時に `I:` サマリ (基準日 = 最後の H、件数は 6 桁ゼロ埋め) | バッチ | 00〜11, 13 |
| G9 | H ごとに出力 H を書き、以降の D は新基準日で計算 (COBOL どおり) | バッチ | 06 |
| G10 | D の前に H が無い、または EOF までに H が無い (0 バイト含む) → `E:`、RC=12 | バッチ | 14, 15 |
| G11 | 残日数の絶対値 10000 以上は下 4 桁 (COBOL どおり)。判定は切り捨て前の値 | バッチ (後処理) | 05 |
| G12 | 編成番号は 2〜7 桁目を trim せずそのまま `calculate` に渡す。`formatRecord` の `%-6s` は 6 桁の値をそのまま出す | バッチ | 11 |
| G13 | 不正日付・数字以外の km・38 桁未満の D → `E:`、RC=12 | バッチ | 16, 17, 18 |
| G14 | 注意日数はバッチ内の定数 14 (`application.properties` は読まない。Web の `urms.inspection.warn-days` はそのまま) | バッチ | 02 |
| G15 | 入出力 ISO-8859-1 (ASCII 範囲は UTF-8 と同一)、LF、最終行も LF。コンソールは UTF-8 明示 | バッチ | 全ケース (バイト比較) |

**G11・G12 の実装場所と画面・API への影響 (§9.3)**

`formatRecord` は画面「検査期限」と `GET /api/batch/inspection-due` (`inspectionDueFile()` → `formatDueFile`) と共用のため、**共用部分の出力は変えない**。

- G11: バッチは `formatRecord(d)` の結果に対し、`|残日数| >= 10000` のときだけ符号 (41 桁目) の直後から末尾 2 桁 (km フラグ・判定) の手前までを `String.format("%04d", Math.abs(残日数) % 10000)` に置き換える。`InspectionDue` の `daysRemaining` を書き換える方式は採らない (判定は切り捨て前の値で済んでいるが、-10000 日を 0 にすると符号が `+` に変わるため)。共用の `formatRecord` は変えないので、画面・API は 10000 日以上で従来どおり 5 桁 (48 桁行) のまま。
- G12: `calculate` / `formatRecord` は編成番号を加工しない。trim しているのは `processFormationsFile` だけなので、バッチが trim せずに渡せば共用部分の変更は不要。画面・API の編成番号は DB 由来で、影響しない。
- `formatHeader` / `formatTrailer` の抽出は出力不変のリファクタリング。`CobolParityTest`・`UrmsApiIT.batchFilesRequireAdmin` (`/api/batch/inspection-due` が `H20261005\nDU3101 ` で始まる) がそのまま通ることで確認する。`calculate` のシグネチャ・結果は変えない (§9.3 C9 の画面・API は影響なし)。

### 10.5 RC・メッセージ対応表 (COBOL → Java)

| RC | 事由 | COBOL のメッセージ | Java のメッセージ | 出力ファイル (Java) | 区分 |
|---|---|---|---|---|---|
| 0 | 正常 (超過なし) | `URINS01 I: 基準日=YYYYMMDD 編成=nnnnnn 注意=nnnnnn 超過=nnnnnn` | 同一 | H/D/T 完全 | COBOL どおり |
| 4 | 正常・超過あり | 同上 | 同一 | H/D/T 完全 | COBOL どおり |
| — | 不明レコード区分 (継続) | `URINS01 W: 不明なレコード区分 x` | 同一 | — | COBOL どおり |
| 8 | トレーラ件数不一致 | `URINS01 E: トレーラ件数不一致` | 同一 | H と処理済み D のみ (T なし) | COBOL どおり |
| 12 | 入力オープンエラー | `URINS01 E: FORMATIONS.DAT OPEN ERROR 35` | `URINS01 E: <入力パス> OPEN ERROR ss` (ファイル無し `35`、読み取り権限なし `37`、その他 `30`)。既定パスでは COBOL と同一文字列 | 作らない (既存ファイルにも触れない) | COBOL どおり (35・37 は GnuCOBOL 3.1.2 で実測。`30` は Java で定義) |
| 12 | H 無し (G10) | (エラーにならない) | `URINS01 E: ヘッダレコード無し` | 残さない (削除) | Java で定義 |
| 12 | 日付不正 (G13) | (不定値で継続) | `URINS01 E: 日付不正 レコード=nnnnnn` | 残さない (削除) | Java で定義 |
| 12 | 走行 km 不正 (G13) | (不定値で継続) | `URINS01 E: 走行KM不正 レコード=nnnnnn` | 残さない (削除) | Java で定義 |
| 12 | D レコード長不足 (G13) | (不定値で継続) | `URINS01 E: レコード長不足 レコード=nnnnnn` | 残さない (削除) | Java で定義 |
| 12 | 出力ファイルの作成・書込失敗 | (検査なし) | `URINS01 E: <出力パス> WRITE ERROR` | 不定 (削除を試みる) | Java で定義 |
| 12 | 引数が 3 個以上 | (該当なし) | `URINS01 E: 引数不正` | 触れない | Java で定義 |
| 12 | 予期しない例外 | (該当なし) | `URINS01 E: 予期しない例外 <例外クラス名>` (スタックトレースは標準エラー) | 残さない (削除を試みる) | Java で定義 |

- `nnnnnn` (レコード) は入力の何レコード目か (1 始まり、6 桁ゼロ埋め)。
- RC=8 / RC=12 では `I:` を表示しない (COBOL と同じ)。それまでに出た `W:` はそのまま残る。
- G10/G13 で出力を削除するのは、RC=12 なら `INSPDUE.DAT` が存在しない (または前回分に触れない) ことを後続の連携 (検修計画システム, §9.6 F2) が前提にできるようにするため。付録 B の mode=error ケースでは「`INSPDUE.DAT` が存在しない」ことも確認する。
- RC 1 は返さない: JVM の未捕捉例外は RC=1 になる (検証 c) ため、`main` で `Throwable` を捕捉して RC=12 に寄せる。JP1 の判定は 0/4/8/12 だけで足りる。
- メッセージの `E:` / `W:` / `I:` の接頭辞と区切りの半角空白は COBOL と同じ。日本語部分は UTF-8 で出す (§10.3)。

### 10.6 Java 8 互換と将来の Java 21 移行

- 使う API は Java 8 標準 (`java.nio.file.Files` / `Paths`, `StandardCharsets`, `PrintStream`, try-with-resources) と既存依存の Joda-Time (`org.joda.time.LocalDate`, `Days`) だけ。`var`・`List.of`・`String.repeat` 等の Java 9+ API は使わない (コンパイルは `java.version=1.8`)。
- 文字コードはすべて明示する。Java 18 以降は既定文字コードが UTF-8 (JEP 400) に変わるが、既定値に依存しないので挙動は変わらない。
- `sun.*`・内部 API・リフレクションは使わない。Java 17 でも同じ起動コマンドで RC=0 を確認済み (検証「Java 17」)。
- Spring Boot 3.2 以降へ上げる場合、ランチャーのクラス名が `org.springframework.boot.loader.launch.PropertiesLauncher` に変わる。そのときはラッパー (§10.7) の 1 行だけを直す。バッチのコードは Spring に依存しないので影響しない。
- 計算は Joda-Time に依存したまま。`java.time` への置き換えは本移行の範囲外 (行う場合は `calculate` の結果が変わらないことを付録 B のケースで確認する)。

### 10.7 JP1 からの起動 (ラッパーと配置)

JP1 ジョブは現在バッチサーバの `/opt/urms/bin/urins01` (COBOL 実行ファイル) を起動して RC を判定している (§9.6 F1)。Java 版は**同じパスに同じ名前のシェルラッパーを置き**、ジョブ定義の起動コマンドを変えずに差し替えられる形にする。ラッパーの作成・配布は s5.1 (Jenkins 切替) で行う。

```sh
#!/bin/sh
# /opt/urms/bin/urins01 : URINS01 編成別 検査期限算出 (Java 版)
URMS_HOME=${URMS_HOME:-/opt/urms}
JAVA=${URMS_JAVA:-java}            # JRE 8 の java。パスは運用部門の確認結果で決める
exec "$JAVA" \
  -Dloader.main=jp.usagi.railway.batch.Urins01Batch \
  -Dloader.path=WEB-INF/classes,WEB-INF/lib \
  -cp "$URMS_HOME/lib/usagi-railway.war" \
  org.springframework.boot.loader.PropertiesLauncher "$@"
```

- `exec` で java に置き換わるため、RC 0/4/8/12 がそのまま JP1 に返る。JP1 側の RC 判定 (0/4 正常, 8/12 異常) は変更不要。
- ラッパーのリポジトリ内テンプレートは `batch/java/urins01`。`batch/java/run.sh` も同じラッパーを使い、`URMS_WAR` を指定できるようにしている。実配布先への配置は s5.1 で行う。
- カレントディレクトリは変えない。JP1 ジョブの作業ディレクトリにある `FORMATIONS.DAT` を読み `INSPDUE.DAT` を書く動作は COBOL と同じ。
- 配置: `/opt/urms/bin/urins01` (ラッパー、実行権限付き)、`/opt/urms/lib/usagi-railway.war` (Jenkins の `Package WAR` の成果物と同一の WAR)。
- ラッパーが日本語を出さないので、ラッパー自体の文字コード設定は不要 (メッセージの UTF-8 化は Java 側で行う)。

**運用部門への依頼事項 (リポジトリ外)**

1. バッチサーバ `urms-bat-stg01` と本番機に JRE 8 があるか確認し、`java` のフルパスを連絡してもらう (§9.6 F3)。無ければ導入を依頼。
2. `/opt/urms/lib/` の作成と `jp1adm` の読み取り権限。
3. JP1 ジョブ定義: 起動コマンドは `/opt/urms/bin/urins01` のまま変更不要の想定。RC 判定 (0/4/8/12) と作業ディレクトリが現行どおりであることの確認、および Java 起動に必要なら環境変数 (`URMS_JAVA`) の追加 (§9.6 F1)。
4. 切替当日の旧 `urins01` (COBOL 実行ファイル) の退避と、切り戻し手順 (旧ファイルを戻すだけで COBOL に戻る)。
5. 標準出力の日本語が UTF-8 で出ることの周知 (JP1 のログ閲覧側の文字コード)。COBOL の GnuCOBOL 版も UTF-8 で出しているため現状と同じ想定だが、確認を依頼する。

### 10.8 テスト設計 (s4.1 / s4.2)

Jenkins は `-Dtest=*Test` / `*IT` に一致するクラスだけを実行する (§9.2 B6) ため、クラス名をこれに合わせる。

| テストクラス | 内容 |
|---|---|
| `jp.usagi.railway.batch.Urins01BatchTest` | `run` の単体テスト: 引数の既定値・3 個以上、CRLF・最終行 LF なし・41 桁以上の入力、出力の LF・末尾空白、`LANG=C` 相当でもメッセージが UTF-8 であること、G11 の後処理 (9999 / 10000 / 12230 / -10000 日) |
| `jp.usagi.railway.batch.Urins01BoundaryParityTest` | `cases.tsv` の 19 ケースを一時ディレクトリで `run` に通す。mode=golden: `INSPDUE.DAT` (有無を含む)・RC・標準出力を `INSPDUE.DAT` / `RC` / `STDOUT.txt` とバイト比較。mode=error: RC=12、標準出力の最終行が `URINS01 E:` で始まる、`INSPDUE.DAT` が存在しない |
| `CobolParityTest` / `UrmsApiIT` (既存) | `formatHeader` / `formatTrailer` 抽出後も無変更で通ること (画面・API 出力の不変確認) |

起動コマンド自体 (WAR + `PropertiesLauncher` + RC の受け渡し) は、Java 版の実行スクリプト `batch/java/run.sh` で実際に WAR を起動して確認する。

### 10.9 起動コマンドの検証結果 (設計時の最小検証)

スクラッチコピー (リポジトリ外) に検証用の main クラス `jp.usagi.railway.batch.LauncherProbe` を一時的に追加し、`JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 mvn -B -q -DskipTests package` で WAR を作って確認した。プローブは `InspectionService.calculate` / `formatRecord` を呼び、引数で指定した終了コードで `System.exit` する。リポジトリには何も追加していない。

| # | 内容 | 結果 |
|---|---|---|
| a | `java -cp target/usagi-railway.war -Dloader.main=...LauncherProbe org.springframework.boot.loader.PropertiesLauncher` (当初案) | RC=1, `ClassNotFoundException` |
| b | a に `-Dloader.path=WEB-INF/classes,WEB-INF/lib` を追加 | 起動成功。Joda-Time は `WEB-INF/lib/joda-time-2.9.9.jar`、`InspectionService` は `WEB-INF/classes` から読込。`DU3101 202612242027011520261015Z20261015+0010 W` を出力 |
| c | b の形で終了コード 0 / 4 / 8 / 12 を指定、および未捕捉例外 | RC=0 / 4 / 8 / 12 がそのまま返る。未捕捉例外は RC=1 |
| d | `LANG=C LC_ALL=C` で起動 | UTF-8 明示の `PrintStream` は日本語を UTF-8 で出力。`System.out` は `file.encoding=ANSI_X3.4-1968` になり日本語が `???` |
| e | 別のカレントディレクトリから WAR を絶対パスで指定 | 成功 (RC=0) |
| f | `java -Dloader.main=...LauncherProbe -jar target/usagi-railway.war` | `WarLauncher` で Web アプリ (Tomcat :8080, `/urms`) が起動。単独 main にならない |
| g | b の形の起動〜終了時間 (3 回) | 各 0.19 秒 |
| h | WAR の Manifest | `Main-Class: org.springframework.boot.loader.WarLauncher`, `Start-Class: jp.usagi.railway.UsagiRailwayApplication`, `PropertiesLauncher` を同梱、Tomcat は `WEB-INF/lib-provided/` |
| Java 17 | b と同じコマンドを OpenJDK 17.0.19 で実行 | RC=0 (将来の Java 移行で起動方式がそのまま使える) |

使用 JDK: OpenJDK 1.8.0_504 (`/usr/lib/jvm/java-8-openjdk-amd64`)。Spring Boot 1.5.22.RELEASE。

### 10.10 実行スクリプトの検証結果 (s3.2)

| 検証 | 結果 |
|---|---|
| 構文・既定 fixture | `sh -n` 成功。WAR の無い状態から初回起動でビルドし、RC=4 / `GOLDEN OK` (14.677 秒) |
| 再実行 | RC=4 / `GOLDEN OK`。0.199 秒、WAR の更新時刻不変 (再ビルドなし) |
| 境界値 19 ケース | mode=golden 14 ケースは RC・`INSPDUE.DAT`・`SYSOUT.TXT` が全件一致。mode=error 5 ケースは Java RC=12、出力なし、最終行 `URINS01 E:` |
| 起動インターフェース | 相対入力パス成功。`/nonexistent` は RC=12 / 終了コード 12。誤った期待ファイルは `GOLDEN MISMATCH` / 終了コード 1。`URMS_WAR` 指定時は再ビルドなし |
| COBOL 既定 fixture | RC=4 / `GOLDEN OK`。Java と `INSPDUE.DAT`・`SYSOUT.TXT` が一致 |

境界値ケースは各ケースの `FORMATIONS.DAT` を `batch/cobol/run.sh <入力> -` と `batch/java/run.sh <入力> -` に与え、`work/RC.TXT`・`work/SYSOUT.TXT`・`work/INSPDUE.DAT` (有無を含む) を比較した。

## 付録 A. 実測ケース (GnuCOBOL 3.1.2 / 既存 Java `processFormationsFile`)

基準日はすべて `20261005`。`|` は行末。COBOL と Java の出力が一致したケースは「一致」と記す。

| # | ケース | COBOL RC | COBOL 出力 / コンソール | Java (`processFormationsFile(lines, 14)`) |
|---|---|---|---|---|
| 00 | golden (`batch/cobol/data/FORMATIONS.DAT`) | 4 | `expected/INSPDUE.DAT` と一致 | 一致 |
| 01 | T 件数 2 / D 1 件 | 8 | `H20261005` と D 1 件のみ (T 無し)。`E: トレーラ件数不一致` | `T000001000001000000` まで出力 |
| 02 | 空行・`Zjunk` 混在 | 0 | `W: 不明なレコード区分 ` と `W: 不明なレコード区分 Z` | 出力一致 (警告なし) |
| 03 | T 無し | 0 | T を出力 (件数 1) | 一致 |
| 04 | T の後ろに D | 0 | 2 件とも出力, T 件数 2 | 一致 |
| 05 | H 2 件 (20261005, 20261101) | 0 | H を 2 回出力, 2 件目は新基準日で計算 | H は `H20261101` 1 件のみ |
| 06 | 残 12231 日 | 0 | `...K20600331+2231 N` | `...K20600331+12231 N` |
| 07 | 境界値 (残 14/15/0/-1 日, 569999/570000/599999/600000 km) | 4 | `W/N/W/X/N/W/W/X(K)` | 一致 |
| 08 | 同日 (K=J, J=Z) | 0 | `K`, `J` | 一致 |
| 09 | うるう日 (2096/02/29+4, 2092/02/29+8, 2000/02/29+8) | 4 | `21000228`, `21000228`, `20080229` | 一致 |
| 11 | H 無し | 0 | D は不定値, H 無し, `基準日=00000000` | `IllegalArgumentException` |
| 12 | 存在しない日付 20230230 | 4 | 交番期限 `16010331`, `X` | `IllegalFieldValueException` |
| 13 | km に `A` | 0 | 処理継続 (`W`) | `NumberFormatException` |
| 14 | 短い D レコード | 4 | 不定な期限 (`09590000` など), `X` | `StringIndexOutOfBoundsException` |
| 15 | 編成番号 ` U31  ` | 0 | `D U31  2026...` | `DU31   2026...` |
| 16 | 入力ファイル無し | 12 | `E: FORMATIONS.DAT OPEN ERROR 35`, `INSPDUE.DAT` 作成なし | (入口なし) |

(ケース 10 は桁ずれ入力の確認用で、14 と同種のため省略)

## 付録 B. 境界値ゴールデンデータ (`src/test/resources/golden/boundary/`)

Java 版のパリティテスト用に、COBOL URINS01 を `batch/cobol/run.sh` で実行して作成した境界値データ。ケース定義は [`cases.tsv`](../../src/test/resources/golden/boundary/cases.tsv)、再生成は COBOL 退役前のコミット `745a86d` で `batch/cobol/gen-boundary.sh` を実行する (GnuCOBOL 3.1.2.0 で実行。再実行しても差分が出ないことを確認済み。COBOL ソースは s5.2 で削除済み)。

各ケースのディレクトリ構成:

| ファイル | 内容 |
|---|---|
| `FORMATIONS.DAT` | 入力 (ケース 12 のみ無し) |
| `INSPDUE.DAT` | COBOL の出力ファイル。作成されなかった場合 (ケース 12) は無し |
| `RC` | COBOL の戻り値 |
| `STDOUT.txt` | COBOL の標準出力 (UTF-8)。行末空白も含めてそのまま (ケース 08) |
| `STDERR.txt` | 標準エラーが空でない場合のみ (ケース 07 の libcob 警告)。ランタイム依存のため比較対象外 |
| `COBOL_RC` / `COBOL_STDOUT.txt` | mode=error のケースのみ。COBOL の実挙動の参考記録で、正解ではない |

- **mode=golden**: Java は `INSPDUE.DAT` (有無を含む)・`RC`・`STDOUT.txt` とバイト一致させる。
- **mode=error**: COBOL の出力は正解にしない (不定値、または H 無し)。Java は `URINS01 E:` で始まるメッセージを出して RC=12 で異常終了すること (§7.2 G10・G13 の決定)。`E:` 以降の文言と出力ファイルの扱いは起動方式の設計で決める (§10.5 で決定: 文言は表のとおり、`INSPDUE.DAT` は残さない)。

基準日は 06 の 2 件目以外すべて `20261005`。

| ケース | mode | 内容 | COBOL RC | 主な期待値 (COBOL 実測) | 関連 |
|---|---|---|---|---|---|
| 00-golden-current | golden | 現行 `batch/cobol/data/FORMATIONS.DAT` | 4 | `expected/INSPDUE.DAT` と同一 | A-00 |
| 01-leap-add-years | golden | 2/29 起点の年加算、交番 +90 日のうるう日跨ぎ | 4 | 20240229+4→`20280229`, 20960229+4→`21000228`, 23960229+4→`24000229`, 21960229+4→`22000228`, 22920229+8→`23000228`, 19920229+8→`20000229`, 交番 20231201→`20240229`, 20241201→`20250301` | §3.4, A-09 |
| 02-days-boundary | golden | 残日数 -1 / 0 / 1 / 14 / 15 | 4 | `-0001 X`, `+0000 W`, `+0001 W`, `+0014 W`, `+0015 N` | §3.7, A-07 |
| 03-km-boundary | golden | 走行 km 境界と判定優先 | 4 | 0・569999 km `N`, 570000・599999 km `W`, 600000・9999999 km `KX`, 残 -1 日 + 570000 km `X` (フラグ空白), 残 14 日 + 600000 km `KX` | §3.7, A-07 |
| 04-same-day-priority | golden | 期限同日の優先順位 | 0 | K=J=Z→`K`, K=J<Z→`K`, J=Z<K→`J`, K=Z<J→`K`, J<K→`J`, Z<J<K→`Z` | §3.5, A-08 |
| 05-days-over-9999 | golden | 残日数 4 桁超 | 4 | 9999 日 `+9999`, 10000 日 `+0000 N`, 12230 日 `+2230`, -10000 日 `-0000 X` | G11, A-06 |
| 06-multi-header | golden | H 2 件 (20261005, 20261101) | 4 | H を 2 回出力、2 件目の D は新基準日で `-0012 X`。`I:` の基準日は `20261101` | G9, A-05 |
| 07-trailer-mismatch | golden | T 件数 3 / D 2 件、T の後ろに D | 8 | H と D 2 件のみ (T 無し、T 後の D は未処理)。`E: トレーラ件数不一致` | G4, G5, A-01 |
| 08-unknown-record | golden | 空行・`Zjunk`・小文字 `h`・先頭空白の D | 0 | 警告 4 行 (区分 空白 / `Z` / `h` / 空白)、D 1 件のみ処理 | G7, A-02 |
| 09-no-trailer | golden | T 無し | 0 | T を出力 (件数 1) | A-03 |
| 10-detail-after-trailer | golden | T の後ろに D | 4 | D 2 件とも出力、T 件数 2 | A-04 |
| 11-formation-spaces | golden | 編成番号 ` U31  ` / `U3101 ` / 全空白 | 0 | 6 桁をそのまま転記 | G12, A-15 |
| 12-no-input-file | golden | 入力ファイル無し | 12 | `E: FORMATIONS.DAT OPEN ERROR 35`、`INSPDUE.DAT` 作成なし | G6, A-16 |
| 13-header-only | golden | H と `T000000` のみ | 0 | `H20261005` と `T000000000000000000` | — |
| 14-empty-file | error | 0 バイトの入力 (H 無し・D 0 件) | (0) | Java は `URINS01 E:` / RC=12 (COBOL は `T000000000000000000` のみを出力し `基準日=00000000`) | G10 |
| 15-no-header | error | H 無しで D 1 件 | (0) | Java は `URINS01 E:` / RC=12 | G10, A-11 |
| 16-invalid-date | error | 存在しない日付 20230230 | (4) | Java は `URINS01 E:` / RC=12 | G13, A-12 |
| 17-km-non-numeric | error | 走行 km に `A` | (0) | Java は `URINS01 E:` / RC=12 | G13, A-13 |
| 18-short-record | error | 38 桁未満の D | (4) | Java は `URINS01 E:` / RC=12 | G13, A-14 |
