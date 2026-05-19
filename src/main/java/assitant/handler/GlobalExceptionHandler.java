package assitant.handler;

// TODO [修复] 全局异常处理器：加 @RestControllerAdvice，捕获 ToolExecutionException / RuntimeException
// 返回统一 JSON 格式 {"code":0,"message":"xxx"}，不要裸抛500+堆栈给客户端
// 参考: @ExceptionHandler(Exception.class) → ResponseEntity<Result>
public class GlobalExceptionHandler {
}
