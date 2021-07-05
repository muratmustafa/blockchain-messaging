package dao.pbft;

public class MsgType {

    public static final int GET_VIEW = -1;

    public static final int CHANGE_VIEW = 0;

    public static final int PRE_PREPARE = 1;

    public static final int PREPARE = 2;

    public static final int COMMIT = 3;

    public static final int CLIENT_REPLAY = 4;
}
