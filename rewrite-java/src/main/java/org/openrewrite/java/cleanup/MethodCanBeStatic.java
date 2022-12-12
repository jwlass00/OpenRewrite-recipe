package org.openrewrite.java.cleanup;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;

public class MethodCanBeStatic extends Recipe {

    @Override
    public String getDisplayName() {
        return "Make eligible methods static";
    }

    @Override
    public String getDescription() {
        return "Makes non-overridable methods (i.e. private or final) that don't access instance data static.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-2325");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration m = super.visitMethodDeclaration(method, ctx);

                // No-op if method is already static || (!final && !private)
                if (m.hasModifier(J.Modifier.Type.Static)
                        || (!m.hasModifier(J.Modifier.Type.Final) && !m.hasModifier(J.Modifier.Type.Private))) {
                    return m;
                }

                Cursor cursor = getCursor();
                if (FindInstanceDataReferencedByMethod.find(getCursorToClass(cursor).getValue(), m).get()) {
                    return m;
                }

                // Determine which modifiers method has so we know how to modify its signature
                List<J.Modifier> modifiers = m.getModifiers();
                Optional<J.Modifier> optFinal = modifiers.stream()
                        .filter(mod -> mod.getType() == J.Modifier.Type.Final)
                        .findAny();

                if (optFinal.isPresent()) { // final
                    m = m.withModifiers(ListUtils.map(m.getModifiers(), mod -> mod.getType() == J.Modifier.Type.Final
                            ? mod.withType(J.Modifier.Type.Static)
                            : mod));
                } else { // private only
                    if (modifiers.stream()
                            .noneMatch(mod -> mod.getType() == J.Modifier.Type.Public)) {

                        J.Modifier mod = new J.Modifier(
                                Tree.randomId(),
                                Space.build(" ", emptyList()),
                                Markers.EMPTY,
                                J.Modifier.Type.Static,
                                Collections.emptyList());
                        m = m.withModifiers(ListUtils.concat(m.getModifiers(), mod));
                    }
                }

                return m;
            }
        };
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class FindInstanceDataReferencedByMethod extends JavaIsoVisitor<AtomicBoolean> {

        J.MethodDeclaration method;

        public static final String METHOD_INVOCATION_NAMES = "Method invocation names";
        public static final String INSTANCE_METHOD_NAMES = "Instance method names";
        public static final String INSTANCE_FIELD_NAMES = "Instance field names";
        public static final String CLASS_NAMES = "Class names";

        /**
         * @param j                 The subtree to search.
         * @param method A {@link J.MethodDeclaration} to check for any instance data access.
         * @return A set of {@link NameTree} locations in this method that reference instance data.
         */
        static AtomicBoolean find(J j, J.MethodDeclaration method) {
            return new FindInstanceDataReferencedByMethod(method)
                    .reduce(j, new AtomicBoolean());
        }

        @Override
        public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean referencesInstanceData) {
            if (referencesInstanceData.get()) {
                return identifier;
            }
            J.Identifier i = super.visitIdentifier(identifier, referencesInstanceData);

            Cursor cursor = getCursor();
            J.MethodDeclaration parentMethod = cursor.firstEnclosing(J.MethodDeclaration.class);
            if (parentMethod == null) {
                if (hasFieldTypeAndIsNotStatic(i)) {  // instance field
                    putName(INSTANCE_FIELD_NAMES, i.getSimpleName());
                } else if (isInnerClassDeclaration(cursor)) {
                    if (hasName(CLASS_NAMES, i.getSimpleName()) && innerClassIsReferenced(i.getSimpleName())) {
                        putName(INSTANCE_METHOD_NAMES, method.getSimpleName());
                        referencesInstanceData.set(true);
                    }
                    putName(CLASS_NAMES, i.getSimpleName());
                }
            } else if (isMethodInvocation(cursor) && hasName(INSTANCE_METHOD_NAMES, i.getSimpleName())) {
                putName(INSTANCE_METHOD_NAMES, parentMethod.getSimpleName());
                referencesInstanceData.set(true);
            } else if (isMethodInvocation(cursor) && isCurrentMethodOrIsCalledByCurrentMethod(parentMethod)) {
                putName(METHOD_INVOCATION_NAMES, i.getSimpleName());
            } else if (isNewClass(cursor)) {
                if (hasName(CLASS_NAMES, i.getSimpleName())) {
                    if (isCurrentMethodOrIsCalledByCurrentMethod(parentMethod)) {
                        referencesInstanceData.set(true);
                        putName(INSTANCE_METHOD_NAMES, parentMethod.getSimpleName());
                    }
                    putName(INSTANCE_METHOD_NAMES, parentMethod.getSimpleName());
                }
                putName(CLASS_NAMES, i.getSimpleName());
            } else if (isInstanceField(i)) {
                putName(INSTANCE_METHOD_NAMES, parentMethod.getSimpleName());
                if (isReferencedDirectlyOrIndirectly(parentMethod.getSimpleName())) {
                    referencesInstanceData.set(true);
                }
            }

            return i;
        }

        private boolean hasFieldTypeAndIsNotStatic(J.Identifier i) {
            return i.getFieldType() != null && !i.getFieldType().hasFlags(Flag.Static);
        }

        private boolean isInnerClassDeclaration(Cursor cursor) {
            return cursor.getParent() != null
                    && cursor.getParent().getValue() instanceof J.ClassDeclaration
                    && cursor.getParent(2) != null
                    && cursor.getParent(2).firstEnclosing(J.ClassDeclaration.class) != null;
        }

        private boolean isMethodInvocation(Cursor cursor) {
            Cursor parent = cursor.getParent();
            return parent != null && parent.getValue() instanceof J.MethodInvocation;
        }

        private boolean isNewClass(Cursor cursor) {
            return cursor.getParent() != null && (cursor.getParent().getValue() instanceof J.NewClass);
        }

        private boolean isInstanceField(J.Identifier i) {
            return hasFieldTypeAndIsNotStatic(i) && hasName(INSTANCE_FIELD_NAMES, i.getSimpleName());
        }

        private boolean isCurrentMethodOrIsCalledByCurrentMethod(J.MethodDeclaration parentMethod) {
            return parentMethod.equals(method) || hasName(METHOD_INVOCATION_NAMES, parentMethod.getSimpleName());
        }

        private boolean innerClassIsReferenced(String newClassName) {
            if (method.getBody() != null) {
                List<Statement> statements = method.getBody().getStatements();
                for (Statement statement : statements) {
                    String str = statement.toString();
                    return str.contains("new " + newClassName + "()") || hasName(METHOD_INVOCATION_NAMES, str.split("\\(")[0]);
                }
            }


            return false;
        }

        private boolean isReferencedDirectlyOrIndirectly(String parentMethodName) {
            return method.getSimpleName().equals(parentMethodName)  // referenced directly by current method (this.method)
                || hasName(METHOD_INVOCATION_NAMES, parentMethodName)  // referenced within method invoked by current method (this.method)
                || innerClassInstanceDataIsReferencedByCurrentMethod();
        }

        /**
         * Finds the top-level method declaration (in case of a method declaration within an anonymous class within
         * a parent method) containing the instance data, then determines whether top-level method is the current
         * method (this.method) or whether the current method calls the top-level method (or any methods that
         * directly or indirectly call the top-level method).
         */
        private boolean innerClassInstanceDataIsReferencedByCurrentMethod() { // reference to instance field within anonymous class
            Cursor currCursor = getCursor();

            do {
                currCursor = currCursor.dropParentUntil(parent -> parent instanceof J.MethodDeclaration);
            } while (currCursor.getParent() != null && currCursor.getParent().firstEnclosing(J.MethodDeclaration.class) != null);

            J.MethodDeclaration md = currCursor.getValue();
            return isCurrentMethodOrIsCalledByCurrentMethod(md);
        }

        private void putName(String message, String name) {
            Cursor currCursor = getCursor();
            do {
                currCursor = currCursor.dropParentUntil(parent -> parent instanceof J.ClassDeclaration);
            } while (currCursor.getParent() != null && currCursor.getParent().firstEnclosing(J.ClassDeclaration.class) != null);

            List<String> names = currCursor.getMessage(message);
            currCursor.putMessage(message, ListUtils.concat(name, names));
        }

        private boolean hasName(String message, String name) {
            List<String> names = getCursorToClass(getCursor()).getNearestMessage(message);
            return names != null && names.contains(name);
        }
    }

    private static Cursor getCursorToClass(Cursor cursor) {
        return cursor.dropParentUntil(is ->
                is instanceof J.ClassDeclaration
        );
    }

}
