package jp.usagi.railway.service;

/**
 * 業務例外の基底クラス. エラーコードは UR-nnnn 形式.
 */
public class UrmsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;

    public UrmsException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
