package com.bumptech.glide.request;

/* JADX INFO: loaded from: classes.dex */
public final class ErrorRequestCoordinator implements RequestCoordinator, Request {
    private volatile Request error;
    private final RequestCoordinator parent;
    private volatile Request primary;
    private final Object requestLock;
    private RequestCoordinator.RequestState primaryState = RequestCoordinator.RequestState.CLEARED;
    private RequestCoordinator.RequestState errorState = RequestCoordinator.RequestState.CLEARED;

    public ErrorRequestCoordinator(Object obj, RequestCoordinator requestCoordinator) {
        this.requestLock = obj;
        this.parent = requestCoordinator;
    }

    private boolean isValidRequestForStatusChanged(Request request) {
        if (this.primaryState != RequestCoordinator.RequestState.FAILED) {
            return request.equals(this.primary);
        }
        return request.equals(this.error) && (this.errorState == RequestCoordinator.RequestState.SUCCESS || this.errorState == RequestCoordinator.RequestState.FAILED);
    }

    private boolean parentCanNotifyCleared() {
        return this.parent == null || this.parent.canNotifyCleared(this);
    }

    private boolean parentCanNotifyStatusChanged() {
        return this.parent == null || this.parent.canNotifyStatusChanged(this);
    }

    private boolean parentCanSetImage() {
        return this.parent == null || this.parent.canSetImage(this);
    }

    @Override // com.bumptech.glide.request.Request
    public void begin() {
        synchronized (this.requestLock) {
            if (this.primaryState != RequestCoordinator.RequestState.RUNNING) {
                this.primaryState = RequestCoordinator.RequestState.RUNNING;
                this.primary.begin();
            }
        }
    }

    @Override // com.bumptech.glide.request.RequestCoordinator
    public boolean canNotifyCleared(Request request) {
        boolean z;
        synchronized (this.requestLock) {
            z = parentCanNotifyCleared() && request.equals(this.primary);
        }
        return z;
    }

    @Override // com.bumptech.glide.request.RequestCoordinator
    public boolean canNotifyStatusChanged(Request request) {
        boolean z;
        synchronized (this.requestLock) {
            z = parentCanNotifyStatusChanged() && isValidRequestForStatusChanged(request);
        }
        return z;
    }

    @Override // com.bumptech.glide.request.RequestCoordinator
    public boolean canSetImage(Request request) {
        boolean zParentCanSetImage;
        synchronized (this.requestLock) {
            zParentCanSetImage = parentCanSetImage();
        }
        return zParentCanSetImage;
    }

    @Override // com.bumptech.glide.request.Request
    public void clear() {
        synchronized (this.requestLock) {
            this.primaryState = RequestCoordinator.RequestState.CLEARED;
            this.primary.clear();
            if (this.errorState != RequestCoordinator.RequestState.CLEARED) {
                this.errorState = RequestCoordinator.RequestState.CLEARED;
                this.error.clear();
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [com.bumptech.glide.request.RequestCoordinator] */
    /* JADX WARN: Type inference failed for: r2v3 */
    /* JADX WARN: Type inference failed for: r2v4 */
    @Override // com.bumptech.glide.request.RequestCoordinator
    public RequestCoordinator getRoot() {
        ?? root;
        synchronized (this.requestLock) {
            RequestCoordinator requestCoordinator = this.parent;
            this = this;
            if (requestCoordinator != null) {
                root = this.parent.getRoot();
            }
        }
        return root;
    }

    @Override // com.bumptech.glide.request.RequestCoordinator, com.bumptech.glide.request.Request
    public boolean isAnyResourceSet() {
        boolean z;
        synchronized (this.requestLock) {
            z = this.primary.isAnyResourceSet() || this.error.isAnyResourceSet();
        }
        return z;
    }

    @Override // com.bumptech.glide.request.Request
    public boolean isCleared() {
        boolean z;
        synchronized (this.requestLock) {
            z = this.primaryState == RequestCoordinator.RequestState.CLEARED && this.errorState == RequestCoordinator.RequestState.CLEARED;
        }
        return z;
    }

    @Override // com.bumptech.glide.request.Request
    public boolean isComplete() {
        boolean z;
        synchronized (this.requestLock) {
            z = this.primaryState == RequestCoordinator.RequestState.SUCCESS || this.errorState == RequestCoordinator.RequestState.SUCCESS;
        }
        return z;
    }

    @Override // com.bumptech.glide.request.Request
    public boolean isEquivalentTo(Request request) {
        if (!(request instanceof ErrorRequestCoordinator)) {
            return false;
        }
        ErrorRequestCoordinator errorRequestCoordinator = (ErrorRequestCoordinator) request;
        return this.primary.isEquivalentTo(errorRequestCoordinator.primary) && this.error.isEquivalentTo(errorRequestCoordinator.error);
    }

    @Override // com.bumptech.glide.request.Request
    public boolean isRunning() {
        boolean z;
        synchronized (this.requestLock) {
            z = this.primaryState == RequestCoordinator.RequestState.RUNNING || this.errorState == RequestCoordinator.RequestState.RUNNING;
        }
        return z;
    }

    @Override // com.bumptech.glide.request.RequestCoordinator
    public void onRequestFailed(Request request) {
        synchronized (this.requestLock) {
            if (request.equals(this.error)) {
                this.errorState = RequestCoordinator.RequestState.FAILED;
                if (this.parent != null) {
                    this.parent.onRequestFailed(this);
                }
            } else {
                this.primaryState = RequestCoordinator.RequestState.FAILED;
                if (this.errorState != RequestCoordinator.RequestState.RUNNING) {
                    this.errorState = RequestCoordinator.RequestState.RUNNING;
                    this.error.begin();
                }
            }
        }
    }

    @Override // com.bumptech.glide.request.RequestCoordinator
    public void onRequestSuccess(Request request) {
        synchronized (this.requestLock) {
            if (request.equals(this.primary)) {
                this.primaryState = RequestCoordinator.RequestState.SUCCESS;
            } else if (request.equals(this.error)) {
                this.errorState = RequestCoordinator.RequestState.SUCCESS;
            }
            if (this.parent != null) {
                this.parent.onRequestSuccess(this);
            }
        }
    }

    @Override // com.bumptech.glide.request.Request
    public void pause() {
        synchronized (this.requestLock) {
            if (this.primaryState == RequestCoordinator.RequestState.RUNNING) {
                this.primaryState = RequestCoordinator.RequestState.PAUSED;
                this.primary.pause();
            }
            if (this.errorState == RequestCoordinator.RequestState.RUNNING) {
                this.errorState = RequestCoordinator.RequestState.PAUSED;
                this.error.pause();
            }
        }
    }

    public void setRequests(Request request, Request request2) {
        this.primary = request;
        this.error = request2;
    }
}
