package qjm.rpc.common;

/**
 * Rpc响应类
 * @author QJM
 *
 */
public class RpcResponse {

    private Throwable error;
    private Object result;

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
