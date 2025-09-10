#include <gtest/gtest.h>
#include "jni_utils.h"
#include <jni.h>

struct FakeEnv {
    int deleted = 0;
    bool exception = false;
    bool described = false;
    bool cleared = false;

    void DeleteLocalRef(jobject) { deleted++; }
    jboolean ExceptionCheck() { return exception ? JNI_TRUE : JNI_FALSE; }
    void ExceptionDescribe() { described = true; }
    void ExceptionClear() { exception = false; cleared = true; }
};

TEST(LocalRefTest, DeletesReferenceOnDestruction) {
    FakeEnv env;
    {
        LocalRef<jobject, FakeEnv> ref(&env, reinterpret_cast<jobject>(0x1));
        EXPECT_EQ(env.deleted, 0);
    }
    EXPECT_EQ(env.deleted, 1);
}

TEST(JniUtilsTest, ClearsException) {
    FakeEnv env;
    env.exception = true;
    bool hadException = checkAndClearException(&env);
    EXPECT_TRUE(hadException);
    EXPECT_TRUE(env.described);
    EXPECT_TRUE(env.cleared);
    EXPECT_FALSE(env.exception);
}

