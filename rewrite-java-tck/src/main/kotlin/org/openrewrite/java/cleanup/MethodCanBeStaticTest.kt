package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.java.Assertions
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

interface MethodCanBeStaticTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(MethodCanBeStatic())
    }

    @Test
    fun finalMethodInstanceDataReversedNotMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {

                public final void test() {
                    i = 1;
                }
                
                private int i;

            }
        """)
    )

    @Test
    fun finalMethodInstanceDataReversedOneMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {

                public final void test() {
                    i = 1;
                }
                
                public final void test2() {
                    int j = 1;
                }
                
                private int i;

            }
        """, """
            class Test {

                public final void test() {
                    i = 1;
                }
                
                public static void test2() {
                    int j = 1;
                }
                
                private int i;

            }
        """)
    )

    @Test
    fun finalMethodNoInstanceDataMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {
                private int i;

                public final void test() {}

            }
        """, """
            class Test {
                private int i;

                public static void test() {}

            }
        """)
    )

    @Test
    fun finalMethodLocalFieldMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {
                private int i;

                public final void test() {
                    int j = 0;
                }

            }
        """, """
            class Test {
                private int i;

                public static void test() {
                    int j = 0;
                }

            }
        """)
    )

    @Test
    fun finalMethodStaticFieldMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {
                private static int i;

                public final void test() {
                    i = i * 2;
                }

            }
        """, """
            class Test {
                private static int i;

                public static void test() {
                    i = i * 2;
                }

            }
        """)
    )

    @Test
    fun finalMethodInstanceDataNotMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {
                private int i;

                public final void test() {
                    i = 1;
                }

            }
        """)
    )

    @Test
    fun privateMethodNoInstanceDataMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {
                private int i;

                private void test() {}

            }
        """, """
            class Test {
                private int i;

                private static void test() {}

            }
        """)
    )

    @Test
    fun privateMethodStaticFieldMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {
                private static int i;

                public final void test() {
                    i = i * 2;
                }

            }
        """, """
            class Test {
                private static int i;

                public static void test() {
                    i = i * 2;
                }

            }
        """)
    )

    @Test
    fun privateMethodInstanceDataNotMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {
                private int i;

                private void test() {
                    i = 1;
                }

            }
        """)
    )

    @Test
    fun privateMethodInvokesMethodNoInstanceDataAllMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {

                private void test() {
                    test2();
                }

                private void test2() {
                    int j = 1;
                }

            }
        """, """
            class Test {

                private static void test() {
                    test2();
                }

                private static void test2() {
                    int j = 1;
                }

            }
        """)
    )

    @Test
    fun privateMethodInvokesInstanceMethodNoneMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {
                private int i;

                private void test() {
                    test2();
                }

                private void test2() {
                    i = 1;
                }

            }
        """)
    )

    @Test
    fun privateMethodInvokesInstanceMethodReversedNoneMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {
                private int i;

                private void test() {
                    i = 1;
                }

                private void test2() {
                    test();
                }

            }
        """)
    )

    @Test
    fun privateMethodChainInvokesNoInstanceDataAllMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {
                private int i;

                private void test() {
                    test2();
                }

                private void test2() {
                   test3();
                }

                private void test3() {
                    int j = 1;
                }

            }
        """, """
            class Test {
                private int i;

                private static void test() {
                    test2();
                }

                private static void test2() {
                   test3();
                }

                private static void test3() {
                    int j = 1;
                }

            }
        """)
    )

    @Test
    fun privateMethodChainInvokesInstanceMethodNotMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {
                private int i;

                private void test() {
                    test2();
                }

                private void test2() {
                   test3();
                }

                private void test3() {
                    i = 1;
                }

            }
        """)
    )

    @Test
    fun privateMethodChainInvokesInstanceMethodReversedNotMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {
                private int i;

                private void test() {
                    i = 1;
                }

                private void test2() {
                   test();
                }

                private void test3() {
                    test2();
                }

            }
        """)
    )

    @Test
    fun privateMethodNotUsingInnerClassMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {

                private void test() {

                }

                class Inner {

                }

            }
        """, """
            class Test {

                private static void test() {

                }

                class Inner {

                }

            }
        """)
    )

    @Test
    fun privateMethodReferencesInnerClassNotMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {

                private void test() {
                    Inner inner = new Inner();
                }

                class Inner {

                }

            }
        """)
    )

    @Test
    fun privateMethodReferencesInnerClassReversedNotMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {
            
                class Inner {

                }

                private void test() {
                    Inner inner = new Inner();
                }

            }
        """)
    )


    @Test
    fun privateMethodInvokesMethoNotReferencingInnerClassBothMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {

                private void test() {
                    test2();
                }
                
                private void test2() {
                
                }

                class Inner {

                }

            }
        """, """
            class Test {

                private static void test() {
                    test2();
                }
                
                private static void test2() {
                
                }

                class Inner {

                }

            }
        """)
    )

    @Test
    fun privateMethodInvokesMethodReferencingInnerClassNotMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {

                private void test() {
                    test2();
                }

                private void test2() {
                    Inner inner = new Inner();
                }

               class Inner {

               }

            }
        """)
    )

    @Test
    fun privateMethodInvokesMethodReferencingInnerClassReversedNotMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {
            
                class Inner {

                }

                private void test() {
                    Inner inner = new Inner();
                }
                
                private void test2() {
                    test();
                }
                
            }
        """)
    )

    @Test
    fun privateMethodReferencesOuterClassMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {

                public final void test() {
                    Foo foo = new Foo();
                }

            }

            class Foo {

            }
        """, """
            class Test {

                public static void test() {
                    Foo foo = new Foo();
                }

            }

            class Foo {

            }
        """)
    )

    @Test
    fun privateMethodAnonymousClassNoInstanceDataMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {

                private void test() {
                    new Runnable() {
                        @Override
                        public void run() {
                            int j = 1;
                        }
                    };
                }
            }
        """, """
            class Test {

                private static void test() {
                    new Runnable() {
                        @Override
                        public void run() {
                            int j = 1;
                        }
                    };
                }
            }
        """)
    )

    @Test
    fun privateMethodAnonymousClassInstanceDataNotMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {
                private int i;

                private void test() {
                    new Runnable() {
                        @Override
                        public void run() {
                            i = 1;
                        }
                    };
                }

            }
        """)
    )

    @Test
    fun privateMethodInvokesMethodContainingAnonymousClassNotAccessingInstanceDataBothMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {
                private int i;

                private void test() {
                    test2();
                }

                private void test2() {
                    new Runnable() {
                        @Override
                        public void run() {
                            int j = 1;
                        }
                    };
                }

            }
        """, """
            class Test {
                private int i;

                private static void test() {
                    test2();
                }

                private static void test2() {
                    new Runnable() {
                        @Override
                        public void run() {
                            int j = 1;
                        }
                    };
                }

            }
        """)
    )

    @Test
    fun privateMethodInvokesMethodContainingAnonymousClassAccessingInstanceDataNeitherMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {
                private int i;

                private void test() {
                    test2();
                }

                private void test2() {
                    new Runnable() {
                        @Override
                        public void run() {
                            i = 1;
                        }
                    };
                }

            }
        """)
    )

    @Test
    fun multiplePrivateMethodsOneMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {
                private int i;

                private void test() {
                    
                }
                
                private void test2() {
                    test3();
                }

                private void test3() {
                    new Runnable() {
                        @Override
                        public void run() {
                            i = 1;
                        }
                    };
                }

            }
        """, """
            class Test {
                private int i;

                private static void test() {
                    
                }
                
                private void test2() {
                    test3();
                }

                private void test3() {
                    new Runnable() {
                        @Override
                        public void run() {
                            i = 1;
                        }
                    };
                }

            }
        """)
    )

    @Test
    fun multiplePrivateMethodsTwoMadeStatic() = rewriteRun(
            Assertions.java("""
            class Test {
                private int i;

                private void test() {
                    test2();
                }
                
                private void test2() {
                    int j = 1;
                }
                
                private void test3() {
                    test4();
                }

                private void test4() {
                    new Runnable() {
                        @Override
                        public void run() {
                            i = 1;
                        }
                    };
                }

            }
        """, """
            class Test {
                private int i;

                private static void test() {
                    test2();
                }
                
                private static void test2() {
                    int j = 1;
                }
                
                private void test3() {
                    test4();
                }

                private void test4() {
                    new Runnable() {
                        @Override
                        public void run() {
                            i = 1;
                        }
                    };
                }

            }
        """)
    )

    @Test
    fun multiplePrivateMethodsComplexCase() = rewriteRun(
            Assertions.java("""
            class Test {
                private int i;

                private void test() {
                    test5();
                }
                
                private void test2() {
                    int j = 1;
                }
                
                private void test3() {
                    new Inner();
                }
                
                private void test4() {
                    test5();
                }

                private void test5() {
                    new Runnable() {
                        @Override
                        public void run() {
                            i = 1;
                        }
                    };
                }
                
                class Inner {

                }

            }
        """, """
            class Test {
                private int i;

                private void test() {
                    test5();
                }
                
                private static void test2() {
                    int j = 1;
                }
                
                private void test3() {
                    new Inner();
                }
                
                private void test4() {
                    test5();
                }

                private void test5() {
                    new Runnable() {
                        @Override
                        public void run() {
                            i = 1;
                        }
                    };
                }
                
                class Inner {

                }

            }
        """)
    )

    @Test
    fun manyPrivateMethodsComplexCaseWithNestedStatements() = rewriteRun(
            Assertions.java("""
            class Test {
                private int i;

                private void test() {
                    test2(i);
                }
                
                private void test2(int num) {
                    int j = num;
                }
                
                private void test3() {
                    new Inner();
                }
                
                private void test4() {
                    test5();
                }

                private void test5() {
                    new Runnable() {
                        @Override
                        public void run() {
                            for (int j = 0; j < 10; j++) {
                                if (j == i) {
                                    System.out.println(j);
                                }
                            }
                        }
                    };
                }
                
                class Inner {

                }

            }
        """, """
            class Test {
                private int i;

                private void test() {
                    test2(i);
                }
                
                private static void test2(int num) {
                    int j = num;
                }
                
                private void test3() {
                    new Inner();
                }
                
                private void test4() {
                    test5();
                }

                private void test5() {
                    new Runnable() {
                        @Override
                        public void run() {
                            for (int j = 0; j < 10; j++) {
                                if (j == i) {
                                    System.out.println(j);
                                }
                            }
                        }
                    };
                }
                
                class Inner {

                }

            }
        """)
    )

}