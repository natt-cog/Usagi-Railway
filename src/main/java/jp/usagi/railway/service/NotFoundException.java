package jp.usagi.railway.service;

public class NotFoundException extends UrmsException {

    private static final long serialVersionUID = 1L;

    public NotFoundException(String what, String key) {
        super("UR-0404", what + "が見つかりません: " + key);
    }
}
