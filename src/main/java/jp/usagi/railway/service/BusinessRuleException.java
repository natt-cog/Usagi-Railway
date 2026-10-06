package jp.usagi.railway.service;

/**
 * 業務ルール違反.
 *
 *   UR-2001 同一遮断器・同一作業日の停電作業が既に申請済み
 *   UR-2002 停電作業の状態遷移が不正
 *   UR-2003 遮断器がトリップ中のため き電停止操作不可
 *   UR-2004 作業時間の指定が不正
 *   UR-2101 警報は確認済み
 *   UR-2201 計測伝文の形式不正
 *   UR-2202 計測伝文のトレーラ件数不一致
 *   UR-3001 予備品 (未搭載) の機器は故障登録不可
 *   UR-3002 修理依頼できない処置状況
 *   UR-3003 修理進捗の後戻りは不可
 *   UR-3004 修理中の故障は完了にできない
 */
public class BusinessRuleException extends UrmsException {

    private static final long serialVersionUID = 1L;

    public BusinessRuleException(String errorCode, String message) {
        super(errorCode, message);
    }
}
