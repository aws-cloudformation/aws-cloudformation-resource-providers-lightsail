package software.amazon.lightsail.certificate;

import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.lightsail.LightsailClient;
import software.amazon.cloudformation.exceptions.*;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Arrays;
import java.util.List;

/**
 * Base Handler will be having the common implementation for the handler.
 */
public abstract class BaseHandlerStd extends BaseHandler<CallbackContext> {

  public final static String InvalidInputException = "InvalidInputException";
  public static final String NotFoundException = "NotFoundException";
  protected final static String InvalidParameterCombination = "InvalidParameterCombination";
  protected static final String InvalidQueryParameter = "InvalidQueryParameter";
  protected final static String InvalidAction = "InvalidAction";
  protected final static String ValidationError = "ValidationError";
  protected static final String MissingParameter = "MissingParameter";

  protected static final String InternalFailure = "InternalFailure";
  protected static final String ServiceUnavailable = "ServiceUnavailable";
  protected static final String OperationFailureException = "OperationFailureException";

  protected static final String ThrottlingException = "ThrottlingException";

  protected final static String NotAuthorized = "NotAuthorized";
  protected static final String OptInRequired = "OptInRequired";
  protected final static String AccessDeniedException = "AccessDeniedException";
  protected static final String UnauthenticatedException = "UnauthenticatedException";

  @Override
  public final ProgressEvent<ResourceModel, CallbackContext> handleRequest(final AmazonWebServicesClientProxy proxy,
                                                                           final ResourceHandlerRequest<ResourceModel> request, final CallbackContext callbackContext,
                                                                           final Logger logger) {

    return handleRequest(proxy, request, callbackContext != null ? callbackContext : new CallbackContext(),
            proxy.newProxy(ClientBuilder::getClient), logger);
  }

  protected abstract ProgressEvent<ResourceModel, CallbackContext> handleRequest(
          final AmazonWebServicesClientProxy proxy, final ResourceHandlerRequest<ResourceModel> request,
          final CallbackContext callbackContext, final ProxyClient<LightsailClient> proxyClient, final Logger logger);

  /**
   * This method handles the exceptions which are caught in the handlers, returns in progress if those error codes are
   * ignored, or retires with a delay if error codes are retryable, or emits an appropriate error code
   *
   * @param e
   *            exception which was caught
   * @param resourceModel
   *            desired resource model for the request
   * @param callbackContext
   *            callback context for current invocation
   * @param ignoreErrorCodes
   *            error codes which should be ignored, i.e. code which should return in-progress
   * @param logger
   *            logger to log statements
   *
   * @return ProgressEvent<ResourceModel, CallbackContext>
   */
  public static ProgressEvent<ResourceModel, CallbackContext> handleError(final Exception e,
                                                                          final ResourceModel resourceModel, final CallbackContext callbackContext,
                                                                          final List<String> ignoreErrorCodes, final Logger logger, final String errorLocation) {

    logger.log(String.format("Error during operation: %s, Error message: %s", errorLocation, e.getMessage()));
    logger.log(Arrays.toString(e.getStackTrace()));

    if (e instanceof AwsServiceException) {
      final String errorCode = ((AwsServiceException) e).awsErrorDetails().errorCode();

      if (ignoreErrorCodes.contains(errorCode)) {
        logger.log("This error is expected at this stage. Continuing execution.");
        return ProgressEvent.progress(resourceModel, callbackContext);
      }
      logger.log(String.format("%s", ((AwsServiceException) e).awsErrorDetails()));

      switch (errorCode) {
        case NotFoundException:
          return ProgressEvent.defaultFailureHandler(new CfnNotFoundException(e),
                  HandlerErrorCode.NotFound);
        case InvalidInputException:
        case InvalidParameterCombination:
        case InvalidQueryParameter:
        case InvalidAction:
        case ValidationError:
        case MissingParameter:
        case OperationFailureException:
          return ProgressEvent.defaultFailureHandler(new CfnInvalidRequestException(e),
                  HandlerErrorCode.InvalidRequest);
        case ThrottlingException:
          return ProgressEvent.defaultFailureHandler(new CfnThrottlingException(e), HandlerErrorCode.Throttling);
        case NotAuthorized:
        case OptInRequired:
        case AccessDeniedException:
        case UnauthenticatedException:
          return ProgressEvent.defaultFailureHandler(new CfnAccessDeniedException(e),
                  HandlerErrorCode.AccessDenied);
        case InternalFailure:
        case ServiceUnavailable:
        default:
          return ProgressEvent.defaultFailureHandler(new CfnGeneralServiceException(e),
                  HandlerErrorCode.GeneralServiceException);
      }
    }
    return ProgressEvent.defaultFailureHandler(new CfnGeneralServiceException(e),
            HandlerErrorCode.GeneralServiceException);
  }
}
