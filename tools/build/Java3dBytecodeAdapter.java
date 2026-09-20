/*
 * RomRaider Open-Source Tuning, Logging and Reflashing
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 */

package com.romraider.build;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * Adapts the legacy Graph3d library to the current JogAmp Java3D packages.
 */
public final class Java3dBytecodeAdapter {
    private Java3dBytecodeAdapter() {
        throw new UnsupportedOperationException();
    }

    public static void main(String[] arguments) throws Exception {
        if (arguments.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: Java3dBytecodeAdapter INPUT.jar OUTPUT.jar");
        }
        adapt(new File(arguments[0]), new File(arguments[1]));
    }

    private static void adapt(File inputFile, File outputFile)
            throws IOException {
        try (JarFile input = new JarFile(inputFile);
                JarOutputStream output = new JarOutputStream(
                        new FileOutputStream(outputFile))) {
            Set<String> writtenEntries = new HashSet<String>();
            Enumeration<JarEntry> entries = input.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!writtenEntries.add(entry.getName())) {
                    continue;
                }

                JarEntry replacement = new JarEntry(entry.getName());
                if (entry.getTime() >= 0) {
                    replacement.setTime(entry.getTime());
                }
                output.putNextEntry(replacement);
                if (!entry.isDirectory()) {
                    byte[] content;
                    try (InputStream entryInput =
                            input.getInputStream(entry)) {
                        content = readAll(entryInput);
                    }
                    if (entry.getName().endsWith(".class")) {
                        content = adaptClass(content);
                    }
                    output.write(content);
                }
                output.closeEntry();
            }
        }
    }

    private static byte[] adaptClass(byte[] content) throws IOException {
        DataInputStream input =
                new DataInputStream(new ByteArrayInputStream(content));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(content.length);
        DataOutputStream output = new DataOutputStream(bytes);

        output.writeInt(input.readInt());
        output.writeShort(input.readUnsignedShort());
        output.writeShort(input.readUnsignedShort());
        int constantPoolCount = input.readUnsignedShort();
        List<ConstantPoolEntry> entries =
                new ArrayList<ConstantPoolEntry>(constantPoolCount);
        entries.add(null);

        for (int index = 1; index < constantPoolCount; index++) {
            int tag = input.readUnsignedByte();
            switch (tag) {
                case 1:
                    entries.add(ConstantPoolEntry.utf8(
                            relocatePackage(input.readUTF())));
                    break;
                case 3:
                case 4:
                    entries.add(ConstantPoolEntry.raw(
                            tag, readBytes(input, 4)));
                    break;
                case 5:
                case 6:
                    entries.add(ConstantPoolEntry.raw(
                            tag, readBytes(input, 8)));
                    entries.add(null);
                    index++;
                    break;
                case 7:
                case 8:
                case 16:
                case 19:
                case 20:
                    entries.add(ConstantPoolEntry.raw(
                            tag, readBytes(input, 2)));
                    break;
                case 9:
                case 10:
                case 11:
                case 12:
                case 17:
                case 18:
                    entries.add(ConstantPoolEntry.raw(
                            tag, readBytes(input, 4)));
                    break;
                case 15:
                    entries.add(ConstantPoolEntry.raw(
                            tag, readBytes(input, 3)));
                    break;
                default:
                    throw new IOException(
                            "Unsupported constant pool tag " + tag);
            }
        }

        if (entries.size() > 65535) {
            throw new IOException("Constant pool is too large");
        }

        output.writeShort(entries.size());
        for (int index = 1; index < entries.size(); index++) {
            ConstantPoolEntry entry = entries.get(index);
            if (entry != null) {
                entry.write(output);
            }
        }
        copy(input, output, input.available());
        output.flush();
        return bytes.toByteArray();
    }

    private static String relocatePackage(String value) {
        return value
                .replace("com/sun/j3d/utils", "org/jogamp/java3d/utils")
                .replace("javax/media/j3d", "org/jogamp/java3d")
                .replace("javax/vecmath", "org/jogamp/vecmath")
                .replace("com.sun.j3d.utils", "org.jogamp.java3d.utils")
                .replace("javax.media.j3d", "org.jogamp.java3d")
                .replace("javax.vecmath", "org.jogamp.vecmath");
    }

    private static byte[] readBytes(DataInputStream input, int count)
            throws IOException {
        byte[] data = new byte[count];
        input.readFully(data);
        return data;
    }

    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private static void copy(DataInputStream input, DataOutputStream output,
            int count) throws IOException {
        output.write(readBytes(input, count));
    }

    private static final class ConstantPoolEntry {
        private final int tag;
        private final byte[] data;
        private final String text;

        private ConstantPoolEntry(int tag, byte[] data, String text) {
            this.tag = tag;
            this.data = data;
            this.text = text;
        }

        private static ConstantPoolEntry raw(int tag, byte[] data) {
            return new ConstantPoolEntry(tag, data, null);
        }

        private static ConstantPoolEntry utf8(String text) {
            return new ConstantPoolEntry(1, null, text);
        }

        private void write(DataOutputStream output) throws IOException {
            output.writeByte(tag);
            if (tag == 1) {
                output.writeUTF(text);
            } else {
                output.write(data);
            }
        }
    }
}
