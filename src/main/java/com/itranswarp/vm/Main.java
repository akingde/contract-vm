package com.itranswarp.vm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import com.itranswarp.contract.ERC20Contract;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

public class Main {

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		ClassReader cr = new ClassReader(Main.class.getResourceAsStream("/com/itranswarp/contract/USDContract.class"));
		ClassWriter cw = new ClassWriter(cr, 0);

		String name = "com.itranswarp.contract.USDContract";
		String rename = name + "$Enhanced";

		ClassVisitor safeMathVisitor = new SafeMathVisitor(cw);
		ClassRemapper renameRemapper = new ClassRemapper(safeMathVisitor, new RenameRemapper(name));

		cr.accept(renameRemapper, 0);
		byte[] enchanced = cw.toByteArray();

		Map<String, byte[]> map = new HashMap<>();
		map.put(rename, enchanced);
		ClassLoader cl = new ByteClassLoader(new URL[0], Main.class.getClassLoader(), map);
		Class<?> cls = cl.loadClass(rename);
		ERC20Contract c = (ERC20Contract) cls.newInstance();
		System.out.println("0x123456: " + c.balanceOf("0x123456"));
		System.out.println("transfer from 0x123456 to 0xff0023 with 123000...");
		c.transfer("0x123456", "0xff0023", 123000);
		System.out.println("0x123456: " + c.balanceOf("0x123456"));
		System.out.println("0xff0023: " + c.balanceOf("0xff0023"));
		// batch transfer:
		System.out.println("batch transfer from 0x123456 to 0xab0912, 0xce52f1 with 998800...");
		c.batchTransfer("0x123456", new String[] { "0xab0912", "0xce52f1" }, 998800);
		System.out.println("0x123456: " + c.balanceOf("0x123456"));
		System.out.println("0xab0912: " + c.balanceOf("0xab0912"));
		System.out.println("0xce52f1: " + c.balanceOf("0xce52f1"));
		// batch transfer with overflow:
		System.out.println("batch transfer from 0x123456 to 0xab0912, 0xce52f1 with " + Long.MAX_VALUE + "...");
		try {
			c.batchTransfer("0x123456", new String[] { "0xab0912", "0xce52f1" }, Long.MAX_VALUE);
		} catch (ArithmeticException e) {
			System.err.println(e);
		}
	}

}

class RenameRemapper extends Remapper {

	String originalName;

	public RenameRemapper(String originalName) {
		this.originalName = originalName.replace('.', '/');
	}

	@Override
	public String map(final String internalName) {
		if (originalName.equals(internalName)) {
			System.out.println("map to: " + internalName + "$Enhanced");
			return internalName + "$Enhanced";
		}
		return super.map(internalName);
	}

}

class SafeMathVisitor extends ClassVisitor {

	public SafeMathVisitor(ClassVisitor visitor) {
		super(Opcodes.ASM6, visitor);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
		return new SafeMathMethodVisitor(mv);
	}
}

class SafeMathMethodVisitor extends MethodVisitor {

	public SafeMathMethodVisitor(MethodVisitor methodVisitor) {
		super(Opcodes.ASM6, methodVisitor);
	}

	@Override
	public void visitInsn(int opcode) {
		switch (opcode) {
		case Opcodes.IADD:
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "addExact", "(II)I", false);
			break;
		case Opcodes.ISUB:
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "subtractExact", "(II)I", false);
			break;
		case Opcodes.IMUL:
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "multiplyExact", "(II)I", false);
			break;
		case Opcodes.LADD:
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "addExact", "(JJ)J", false);
			break;
		case Opcodes.LSUB:
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "subtractExact", "(JJ)J", false);
			break;
		case Opcodes.LMUL:
			mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Math", "multiplyExact", "(JJ)J", false);
			break;
		default:
			mv.visitInsn(opcode);
			break;
		}
	}

}

class ByteClassLoader extends URLClassLoader {

	final Map<String, byte[]> extraClassDefs;

	public ByteClassLoader(URL[] urls, ClassLoader parent, Map<String, byte[]> extraClassDefs) {
		super(urls, parent);
		this.extraClassDefs = new HashMap<>(extraClassDefs);
	}

	@Override
	protected Class<?> findClass(final String name) throws ClassNotFoundException {
		byte[] classBytes = this.extraClassDefs.remove(name);
		if (classBytes != null) {
			System.out.println("Loaded class: " + name);
			return defineClass(name, classBytes, 0, classBytes.length);
		}
		return super.findClass(name);
	}

}
