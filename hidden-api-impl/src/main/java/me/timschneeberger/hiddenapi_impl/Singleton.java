package me.timschneeberger.hiddenapi_impl;

public abstract class Singleton<T> {

    private T mInstance;

    protected abstract T create();

    public final T get() {
        synchronized (this) {
            if (mInstance == null) {
                try {
                    mInstance = create();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return mInstance;
        }
    }

    public final T getOrThrow() {
        T instance = get();
        if(instance == null)
            throw new NullPointerException();
        return instance;
    }
}