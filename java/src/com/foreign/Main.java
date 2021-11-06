package com.foreign;

import jdk.incubator.foreign.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Optional;

import static jdk.incubator.foreign.ResourceScope.newImplicitScope;

public class Main {
    public static void main(String[] args) throws Throwable {
        var libPath = Optional.ofNullable(args[0])
                .orElseThrow(() -> new IllegalArgumentException("No library path provided!!!"));
        System.load(libPath);

        var loaderLookup = SymbolLookup.loaderLookup();
        var print = loaderLookup.lookup("print_from_rust").orElseThrow();
        var executeRust = loaderLookup.lookup("get_string_in_rust").orElseThrow();
        var getStringFromRust = loaderLookup.lookup("return_string_from_rust").orElseThrow();
        var change_by_ref = loaderLookup.lookup("call_back").orElseThrow();

        var printInRust = CLinker.getInstance().downcallHandle(
                print,
                MethodType.methodType(void.class),
                FunctionDescriptor.ofVoid()
        );
        printInRust.invokeExact();

        var sendToRust = CLinker.getInstance().downcallHandle(
                executeRust,
                MethodType.methodType(void.class, MemoryAddress.class),
                FunctionDescriptor.ofVoid(
                        CLinker.C_POINTER
                )
        );
        var str = CLinker.toCString("Java String", newImplicitScope());
        sendToRust.invokeExact(str.address());


        var getFromRust = CLinker.getInstance().downcallHandle(
                getStringFromRust,
                MethodType.methodType(MemoryAddress.class, MemoryAddress.class),
                FunctionDescriptor.of(
                        CLinker.C_POINTER,
                        CLinker.C_POINTER
                )
        );
        var javaStr = CLinker.toCString("from java", newImplicitScope());
        var memory = (MemoryAddress) getFromRust.invokeExact(javaStr.address());
        var res = memory.asSegment(16, newImplicitScope());
        System.out.printf("JAVA %s", new String(res.toByteArray()));


        class CallFromRust {
            static MemoryAddress getString(MemoryAddress addr) {
                return addr;
            }
        }
        MethodHandle stringHandle
                = MethodHandles.lookup()
                .findStatic(CallFromRust.class, "getString",
                        MethodType.methodType(MemoryAddress.class,
                                MemoryAddress.class));
        MemoryAddress javaFuncPointer =
                CLinker.getInstance().upcallStub(stringHandle,
                        FunctionDescriptor.of(
                                CLinker.C_POINTER,
                                CLinker.C_POINTER),
                        newImplicitScope());
        MethodHandle executeJavaFromRust = CLinker.getInstance().downcallHandle(
                change_by_ref,
                MethodType.methodType(MemoryAddress.class, MemoryAddress.class),
                FunctionDescriptor.of(
                        CLinker.C_POINTER,
                        CLinker.C_POINTER
                )
        );
        var javaMemory = (MemoryAddress)executeJavaFromRust.invoke(javaFuncPointer);
        var resJavaFunc = javaMemory.asSegment(18, newImplicitScope());
        System.out.printf("JAVA %S", new String(resJavaFunc.toByteArray()));
    }
}
