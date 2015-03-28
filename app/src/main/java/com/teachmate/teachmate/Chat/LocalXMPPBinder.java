package com.teachmate.teachmate.Chat;

import android.os.Binder;

import java.lang.ref.WeakReference;

/**
 * Created by VVekariya on 28-Mar-15.
 */
public class LocalXMPPBinder<S> extends Binder {
    private final WeakReference<S> mService;

    public LocalXMPPBinder(final S service) {
        mService = new WeakReference<S>(service);
    }

    public S getService() {
        return mService.get();
    }

}
