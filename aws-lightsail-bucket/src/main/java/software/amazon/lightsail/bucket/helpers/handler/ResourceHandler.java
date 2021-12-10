package software.amazon.lightsail.bucket.helpers.handler;

import lombok.val;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.lightsail.bucket.CallbackContext;
import software.amazon.lightsail.bucket.ResourceModel;

public abstract class ResourceHandler {

    public ProgressEvent<ResourceModel, CallbackContext> handleCreate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val sam = preCreate(progress).then(this::create);
        return sam;
    }

    public ProgressEvent<ResourceModel, CallbackContext> handleUpdate(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val sam = preUpdate(progress).then(this::update);
        return sam;
    }

    public ProgressEvent<ResourceModel, CallbackContext> handleDelete(
            final ProgressEvent<ResourceModel, CallbackContext> progress) {
        val sam = preDelete(progress).then(this::delete);
        return sam;
    }

    public boolean isStabilized(final CallbackContext callbackContext, final String step) {
        if (callbackContext.isWaitCountReached(step)) {
            return true; // When wait count is reached treat as Stabilized.
        }
        // Increment the wait count for the step.
        callbackContext.incrementWaitCount(step);
        return false;
    }

    protected abstract ProgressEvent<ResourceModel, CallbackContext> preCreate(
            final ProgressEvent<ResourceModel, CallbackContext> progress);

    protected abstract ProgressEvent<ResourceModel, CallbackContext> preUpdate(
            final ProgressEvent<ResourceModel, CallbackContext> progress);

    protected abstract ProgressEvent<ResourceModel, CallbackContext> preDelete(
            final ProgressEvent<ResourceModel, CallbackContext> progress);

    protected abstract ProgressEvent<ResourceModel, CallbackContext> create(
            final ProgressEvent<ResourceModel, CallbackContext> progress);

    protected abstract ProgressEvent<ResourceModel, CallbackContext> update(
            final ProgressEvent<ResourceModel, CallbackContext> progress);

    protected abstract ProgressEvent<ResourceModel, CallbackContext> delete(
            final ProgressEvent<ResourceModel, CallbackContext> progress);
}
