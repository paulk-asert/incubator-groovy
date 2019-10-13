package org.codehaus.groovy.tools.groovydoc.antlr4;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.codehaus.groovy.groovydoc.GroovyClassDoc;
import org.codehaus.groovy.groovydoc.GroovyMethodDoc;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.tools.groovydoc.SimpleGroovyAbstractableElementDoc;
import org.codehaus.groovy.tools.groovydoc.SimpleGroovyClassDoc;
import org.codehaus.groovy.tools.groovydoc.SimpleGroovyConstructorDoc;
import org.codehaus.groovy.tools.groovydoc.SimpleGroovyDoc;
import org.codehaus.groovy.tools.groovydoc.SimpleGroovyExecutableMemberDoc;
import org.codehaus.groovy.tools.groovydoc.SimpleGroovyFieldDoc;
import org.codehaus.groovy.tools.groovydoc.SimpleGroovyMethodDoc;
import org.codehaus.groovy.tools.groovydoc.SimpleGroovyParameter;
import org.codehaus.groovy.tools.groovydoc.SimpleGroovyProgramElementDoc;
import org.codehaus.groovy.tools.groovydoc.SimpleGroovyType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroovydocJavaVisitor extends VoidVisitorAdapter<Object> {
    private SimpleGroovyClassDoc currentClassDoc = null;
    private Map<String, GroovyClassDoc> classDocs = new HashMap<>();
    private String packagePath;
    private static final String FS = "/";

    public GroovydocJavaVisitor(String packagePath) {
        this.packagePath = packagePath;
    }

    @Override
    public void visit(AnnotationDeclaration n, Object arg) {
        List<String> imports = new ArrayList<>();
        currentClassDoc = new SimpleGroovyClassDoc(imports, n.getNameAsString());
        setModifiers(n.getModifiers(), currentClassDoc);
        currentClassDoc.setTokenType(SimpleGroovyDoc.ANNOTATION_DEF);
        currentClassDoc.setFullPathName(withSlashes(packagePath + FS + n.getNameAsString()));
        n.getJavadocComment().ifPresent(javadocComment ->
                currentClassDoc.setRawCommentText(javadocComment.getContent()));
        classDocs.put(currentClassDoc.getFullPathName(), currentClassDoc);
        super.visit(n, arg);
    }

    @Override
    public void visit(EnumDeclaration n, Object arg) {
        List<String> imports = new ArrayList<>();
        currentClassDoc = new SimpleGroovyClassDoc(imports, n.getNameAsString());
        setModifiers(n.getModifiers(), currentClassDoc);
        currentClassDoc.setTokenType(SimpleGroovyDoc.ENUM_DEF);
        currentClassDoc.setFullPathName(withSlashes(packagePath + FS + n.getNameAsString()));
        n.getJavadocComment().ifPresent(javadocComment ->
                currentClassDoc.setRawCommentText(javadocComment.getContent()));
        classDocs.put(currentClassDoc.getFullPathName(), currentClassDoc);
        super.visit(n, arg);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Object arg) {
        List<String> imports = new ArrayList<>();
        currentClassDoc = new SimpleGroovyClassDoc(imports, n.getNameAsString());
        setModifiers(n.getModifiers(), currentClassDoc);
        if (n.isInterface()) {
            currentClassDoc.setTokenType(SimpleGroovyDoc.INTERFACE_DEF);
        }
        if (n.getExtendedTypes().size() > 0) {
            currentClassDoc.setSuperClassName(n.getExtendedTypes(0).getNameAsString());
        }
        n.getImplementedTypes().forEach(classOrInterfaceType ->
                currentClassDoc.addInterfaceName(classOrInterfaceType.getNameAsString()));
        currentClassDoc.setFullPathName(withSlashes(packagePath + FS + n.getNameAsString()));
        n.getJavadocComment().ifPresent(javadocComment ->
                currentClassDoc.setRawCommentText(javadocComment.getContent()));
        classDocs.put(currentClassDoc.getFullPathName(), currentClassDoc);
        super.visit(n, arg);
    }

    private void setModifiers(NodeList<Modifier> modifiers, SimpleGroovyAbstractableElementDoc elementDoc) {
        if (modifiers.contains(Modifier.publicModifier())) {
            elementDoc.setPublic(true);
        }
        if (modifiers.contains(Modifier.staticModifier())) {
            elementDoc.setStatic(true);
        }
        if (modifiers.contains(Modifier.abstractModifier())) {
            elementDoc.setAbstract(true);
        }
        if (modifiers.contains(Modifier.finalModifier())) {
            elementDoc.setFinal(true);
        }
        if (modifiers.contains(Modifier.protectedModifier())) {
            elementDoc.setProtected(true);
        }
        if (modifiers.contains(Modifier.privateModifier())) {
            elementDoc.setPrivate(true);
        }
    }

    private String withSlashes(String s) {
        return s.replaceAll("\\.", "/");
    }

    @Override
    public void visit(MethodDeclaration n, Object arg) {
        SimpleGroovyMethodDoc meth = new SimpleGroovyMethodDoc(n.getNameAsString(), currentClassDoc);
        System.out.println(DefaultGroovyMethods.dump(n.getType()));
        meth.setReturnType(new SimpleGroovyType(withSlashes(n.getTypeAsString())));
        setConstructorOrMethodCommon(n, meth);
        currentClassDoc.add(meth);
//        processPropertiesFromGetterSetter(meth);
        super.visit(n, arg);
    }

    @Override
    public void visit(ConstructorDeclaration n, Object arg) {
        SimpleGroovyConstructorDoc meth = new SimpleGroovyConstructorDoc(n.getNameAsString(), currentClassDoc);
        setConstructorOrMethodCommon(n, meth);
        currentClassDoc.add(meth);
        super.visit(n, arg);
    }

    private void setConstructorOrMethodCommon(CallableDeclaration<? extends CallableDeclaration> n, SimpleGroovyExecutableMemberDoc methOrCons) {
        n.getJavadocComment().ifPresent(javadocComment ->
                methOrCons.setRawCommentText(javadocComment.getContent()));
        setModifiers(n.getModifiers(), methOrCons);
        for (Parameter param : n.getParameters()) {
            SimpleGroovyParameter p = new SimpleGroovyParameter(param.getNameAsString());
            p.setTypeName(withSlashes(param.getTypeAsString()));
            methOrCons.add(p);
        }
    }

    private void processPropertiesFromGetterSetter(SimpleGroovyMethodDoc currentMethodDoc) {
        String methodName = currentMethodDoc.name();
        int len = methodName.length();
        String prefix = null;
        String propName = null;
        if (len > 3 && methodName.startsWith("get")) {
            prefix = "get";
            propName = methodName.substring(3);
        } else if (len > 3 && methodName.startsWith("set")) {
            prefix = "set";
            propName = methodName.substring(3);
        } else if (len > 2 && methodName.startsWith("is")) {
            prefix = "is";
            propName = methodName.substring(2);
        } else {
            // Not a (get/set/is) method that contains a property name
            return;
        }

        SimpleGroovyClassDoc classDoc = currentClassDoc;
        // TODO: not sure why but groovy.ui.view.BasicContentPane#buildOutputArea classDoc is null
        if (classDoc == null) {
            return;
        }
        GroovyMethodDoc methods[] = classDoc.methods();

        //find expected method name
        String expectedMethodName = null;
        if ("set".equals(prefix) && (currentMethodDoc.parameters().length >= 1 && !currentMethodDoc.parameters()[0].typeName().equals("boolean"))) {
            expectedMethodName = "get" + propName;
        } else if ("get".equals(prefix) && !currentMethodDoc.returnType().typeName().equals("boolean")) {
            expectedMethodName = "set" + propName;
        } else if ("is".equals(prefix)) {
            expectedMethodName = "set" + propName;
        } else {
            expectedMethodName = "is" + propName;
        }

        for (GroovyMethodDoc methodDoc : methods) {
            if (methodDoc.name().equals(expectedMethodName)) {

                //extract the field name
                String fieldName = propName.substring(0, 1).toLowerCase() + propName.substring(1);
                SimpleGroovyFieldDoc currentFieldDoc = new SimpleGroovyFieldDoc(fieldName, classDoc);

                //find the type of the field; if it's a setter, need to get the type of the params
                if(expectedMethodName.startsWith("set") && methodDoc.parameters().length >= 1) {
                    String typeName = methodDoc.parameters()[0].typeName();
                    currentFieldDoc.setType(new SimpleGroovyType(typeName));
                } else {
                    //if it's not setter, get the type info of the return type of the get* method
                    currentFieldDoc.setType(methodDoc.returnType());
                }

                if (methodDoc.isPublic() && currentMethodDoc.isPublic()) {
                    classDoc.addProperty(currentFieldDoc);
                    break;
                }
            }
        }
    }

    @Override
    public void visit(FieldDeclaration n, Object arg) {
        String name = n.getVariable(0).getNameAsString();
        SimpleGroovyFieldDoc field = new SimpleGroovyFieldDoc(name, currentClassDoc);
        field.setType(new SimpleGroovyType(withSlashes(n.getVariable(0).getTypeAsString())));
        setModifiers(n.getModifiers(), field);
        n.getJavadocComment().ifPresent(javadocComment ->
                field.setRawCommentText(javadocComment.getContent()));
        currentClassDoc.add(field);
        super.visit(n, arg);
    }

    public Map<String, GroovyClassDoc> getGroovyClassDocs() {
        return classDocs;
    }

}
