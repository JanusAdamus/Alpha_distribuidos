package mx.unam.alpha.common.model;

public class TcpRequest {

    private RequestType type;
    private String username;
    private String password;
    private Integer row;
    private Integer col;
    private Long monsterId;

    public static TcpRequest register(String username, String password) {
        TcpRequest request = new TcpRequest();
        request.type = RequestType.REGISTER;
        request.username = username;
        request.password = password;
        return request;
    }

    public static TcpRequest login(String username, String password) {
        TcpRequest request = new TcpRequest();
        request.type = RequestType.LOGIN;
        request.username = username;
        request.password = password;
        return request;
    }

    public static TcpRequest hit(int row, int col, long monsterId) {
        TcpRequest request = new TcpRequest();
        request.type = RequestType.HIT;
        request.row = row;
        request.col = col;
        request.monsterId = monsterId;
        return request;
    }

    public static TcpRequest gameState() {
        TcpRequest request = new TcpRequest();
        request.type = RequestType.GAME_STATE;
        return request;
    }

    public static TcpRequest logout() {
        TcpRequest request = new TcpRequest();
        request.type = RequestType.LOGOUT;
        return request;
    }

    public static TcpRequest ping() {
        TcpRequest request = new TcpRequest();
        request.type = RequestType.PING;
        return request;
    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public Integer getCol() {
        return col;
    }

    public void setCol(Integer col) {
        this.col = col;
    }

    public Long getMonsterId() {
        return monsterId;
    }

    public void setMonsterId(Long monsterId) {
        this.monsterId = monsterId;
    }
}
