package com.example.cso;

import org.junit.Test;
import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

public class TestRunner  extends BlockJUnit4ClassRunner {
    public TestRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        Object testClass;
        try {
            testClass = new ReflectiveCallable() {
                @Override
                protected Object runReflectiveCall() throws Throwable {
                    return createTest();
                }
            }.run();
        } catch (Throwable e) {
            runChild(new FrameworkMethod(method.getMethod()), notifier);
            return;
        }

        Description description = describeChild(method);
        if(method.getAnnotation(Test.class) != null){
            runTestMethod(methodBlock(method), notifier, description);
        }
    }

    private void runTestMethod(Statement statement, RunNotifier notifier, Description description){
        try{
            statement.evaluate();
            System.out.println(description.getDisplayName() + ": Success");
        }catch (Throwable e){
            System.out.println(description.getDisplayName() + ": Fail - " + e.getMessage());
            notifier.fireTestFailure(new Failure(description, e));
        }
    }
}
