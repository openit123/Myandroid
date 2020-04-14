//
// Created by ching fung wu on 2019-12-03.
//

#include <stdio.h>
#include <jni.h>
#include "JavaToC.h"
JNIEXPORT void JNICALL Java_JavaToC_helloC(JNIEnv *env, jobject javaobj)
{
	printf("Hello World: From C");
	return;
}