#pragma once
#include <jni.h>

// RAII wrapper for JNI local references.
// Deletes the local reference when the object goes out of scope.
template <typename T, typename Env = JNIEnv>
class LocalRef {
public:
    LocalRef(Env* env, T obj) : env_(env), obj_(obj) {}
    ~LocalRef() {
        if (obj_) {
            env_->DeleteLocalRef(obj_);
        }
    }
    T get() const { return obj_; }
    operator T() const { return obj_; }
    T release() {
        T tmp = obj_;
        obj_ = nullptr;
        return tmp;
    }
    // disable copy
    LocalRef(const LocalRef&) = delete;
    LocalRef& operator=(const LocalRef&) = delete;
    // allow move
    LocalRef(LocalRef&& other) noexcept : env_(other.env_), obj_(other.obj_) { other.obj_ = nullptr; }
    LocalRef& operator=(LocalRef&& other) noexcept {
        if (this != &other) {
            if (obj_) {
                env_->DeleteLocalRef(obj_);
            }
            env_ = other.env_;
            obj_ = other.obj_;
            other.obj_ = nullptr;
        }
        return *this;
    }
private:
    Env* env_;
    T obj_;
};

// Helper to check for JNI exceptions. Returns true if an exception was present.
template <typename Env>
inline bool checkAndClearException(Env* env) {
    if (env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
        return true;
    }
    return false;
}

