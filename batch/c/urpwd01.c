/*
 * ============================================================================
 *  うさぎ鉄道 電力管理 日次バッチ
 *  URPWD01 : 変電所別 電力日報 作成
 *
 *  入力 : TLM_YYYYMMDD.DAT   RTU 計測伝文 (1 時間値) 収集ファイル
 *           H レコード : 'H' + 計測日(8)
 *           D レコード : 'D' + 変電所(4) + 時刻HHMM(4) + 電圧V(5) + 電流A(5)
 *                        + 電力量kWh(7) + 品質(1: '0'=正常 '1'=欠測)
 *           T レコード : 'T' + D レコード件数(6)
 *  出力 : DAILY_YYYYMMDD.DAT 電力日報 (指令所 帳票サーバへ FTP 転送)
 *           H レコード : 'H' + 計測日(8)
 *           D レコード : 'D' + 変電所(4) + 有効件数(2) + 最低電圧(5) + 平均電圧(5)
 *                        + 最大電流(5) + 電力量計(9) + 電圧低下回数(2)
 *                        + 過電流回数(2) + 欠測件数(2)
 *           T レコード : 'T' + 変電所数(4) + 総電力量(11)
 *
 *  戻り値 : 0 正常 / 4 欠測あり / 8 トレーラ件数不一致 / 12 入出力エラー
 *
 *  変更履歴
 *    2009/04/01 初版 (HP-UX 11i / aCC)                 うさぎ電機システムズ
 *    2012/07/10 欠測件数の出力追加                       (佐藤)
 *    2016/02/15 RHEL6 移行 (gcc 4.4)                    (山本)
 *    2019/11/05 過電流判定 追加                           (山本)
 * ============================================================================
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define MAX_SS        64
#define REC_LEN       128

/* 警報判定閾値 (直流 1500V き電)  ※ application.properties の urms.alarm.* と合わせること */
#define UV_THRESHOLD  1350   /* 電圧低下 [V]  未満で計上 */
#define OC_THRESHOLD  4000   /* 過電流   [A]  超過で計上 */

#define RC_OK         0
#define RC_WARN       4
#define RC_TRAILER    8
#define RC_IOERR      12

typedef struct {
    char ss_code[5];
    int  count;          /* 有効件数 */
    int  min_v;
    long sum_v;
    int  max_i;
    long energy;
    int  uv_count;
    int  oc_count;
    int  missing;
} SS_SUMMARY;

static SS_SUMMARY tbl[MAX_SS];
static int        tbl_cnt = 0;

/* 固定長項目を数値に変換 (atoi は桁指定できないため一旦コピー) */
static long fld_num(const char *rec, int pos, int len)
{
    char buf[16];
    memset(buf, 0, sizeof(buf));
    memcpy(buf, rec + pos, len);
    return atol(buf);
}

static SS_SUMMARY *find_ss(const char *code)
{
    int i, j;
    for (i = 0; i < tbl_cnt; i++) {
        int c = strncmp(tbl[i].ss_code, code, 4);
        if (c == 0) return &tbl[i];
        if (c > 0) break;
    }
    if (tbl_cnt >= MAX_SS) {
        fprintf(stderr, "URPWD01 E: 変電所数が上限 (%d) を超えました\n", MAX_SS);
        exit(RC_IOERR);
    }
    /* 変電所コード順に挿入 */
    for (j = tbl_cnt; j > i; j--) tbl[j] = tbl[j - 1];
    memset(&tbl[i], 0, sizeof(SS_SUMMARY));
    memcpy(tbl[i].ss_code, code, 4);
    tbl[i].min_v = 99999;
    tbl_cnt++;
    return &tbl[i];
}

int main(int argc, char *argv[])
{
    FILE *in, *out;
    char  rec[REC_LEN];
    char  date[9] = "";
    char  outname[64];
    long  d_count = 0, trl_count = -1, grand = 0;
    long  miss_total = 0;   /* きつね交通 2015/03 追加: 欠測合計をトレーラに出力 */
    int   i, rc = RC_OK, any_missing = 0;

    if (argc < 2) {
        fprintf(stderr, "usage: urpwd01 TLM_YYYYMMDD.DAT [出力ディレクトリ]\n");
        return RC_IOERR;
    }
    if ((in = fopen(argv[1], "r")) == NULL) {
        perror(argv[1]);
        return RC_IOERR;
    }

    while (fgets(rec, sizeof(rec), in) != NULL) {
        rec[strcspn(rec, "\r\n")] = '\0';
        switch (rec[0]) {
        case 'H':
            strncpy(date, rec + 1, 8);
            date[8] = '\0';
            break;
        case 'D': {
            char code[5];
            SS_SUMMARY *s;
            int v, a;
            strncpy(code, rec + 1, 4);
            code[4] = '\0';
            s = find_ss(code);
            d_count++;
            if (rec[26] == '1') {
                s->missing++;
                any_missing = 1;
                break;
            }
            v = (int) fld_num(rec, 9, 5);
            a = (int) fld_num(rec, 14, 5);
            s->count++;
            s->sum_v += v;
            if (v < s->min_v) s->min_v = v;
            if (a > s->max_i) s->max_i = a;
            s->energy += fld_num(rec, 19, 7);
            if (v < UV_THRESHOLD) s->uv_count++;
            if (a > OC_THRESHOLD) s->oc_count++;
            break;
        }
        case 'T':
            trl_count = fld_num(rec, 1, 6);
            break;
        default:
            fprintf(stderr, "URPWD01 W: 不明なレコード区分 [%c]\n", rec[0]);
        }
    }
    fclose(in);

    if (trl_count != d_count) {
        fprintf(stderr, "URPWD01 E: トレーラ件数不一致 (T=%ld, D=%ld)\n", trl_count, d_count);
        return RC_TRAILER;
    }

    sprintf(outname, "%s/DAILY_%s.DAT", argc > 2 ? argv[2] : ".", date);
    if ((out = fopen(outname, "w")) == NULL) {
        perror(outname);
        return RC_IOERR;
    }
    fprintf(out, "H%s\n", date);
    for (i = 0; i < tbl_cnt; i++) {
        SS_SUMMARY *s = &tbl[i];
        int avg = s->count > 0 ? (int) (s->sum_v / s->count) : 0;
        int minv = s->count > 0 ? s->min_v : 0;
        fprintf(out, "D%-4.4s%02d%05d%05d%05d%09ld%02d%02d%02d\n",
                s->ss_code, s->count, minv, avg, s->max_i, s->energy,
                s->uv_count, s->oc_count, s->missing);
        grand += s->energy;
        miss_total += s->missing;
    }
    fprintf(out, "T%04d%011ld%04ld\n", tbl_cnt, grand, miss_total);
    fclose(out);

    printf("URPWD01 I: 計測日=%s 入力=%ld件 変電所=%d 総電力量=%ldkWh\n", date, d_count, tbl_cnt, grand);
    if (any_missing) rc = RC_WARN;
    return rc;
}
