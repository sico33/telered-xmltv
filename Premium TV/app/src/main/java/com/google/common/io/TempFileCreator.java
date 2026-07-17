package com.google.common.io;

import com.google.common.base.StandardSystemProperty;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryFlag;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/* JADX INFO: loaded from: classes.dex */
@ElementTypesAreNonnullByDefault
abstract class TempFileCreator {
    static final TempFileCreator INSTANCE = pickSecureCreator();

    private static final class JavaIoCreator extends TempFileCreator {
        private static final int TEMP_DIR_ATTEMPTS = 10000;

        private JavaIoCreator() {
            super();
        }

        @Override // com.google.common.io.TempFileCreator
        File createTempDir() {
            File file = new File(StandardSystemProperty.JAVA_IO_TMPDIR.value());
            String str = System.currentTimeMillis() + "-";
            for (int i = 0; i < 10000; i++) {
                File file2 = new File(file, str + i);
                if (file2.mkdir()) {
                    return file2;
                }
            }
            throw new IllegalStateException("Failed to create directory within 10000 attempts (tried " + str + "0 to " + str + "9999)");
        }

        @Override // com.google.common.io.TempFileCreator
        File createTempFile(String str) throws IOException {
            return File.createTempFile(str, null, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    static final class JavaNioCreator extends TempFileCreator {
        private static final PermissionSupplier directoryPermissions;
        private static final PermissionSupplier filePermissions;

        /* JADX INFO: Access modifiers changed from: private */
        interface PermissionSupplier {
            FileAttribute<?> get() throws IOException;
        }

        static {
            Set<String> setSupportedFileAttributeViews = FileSystems.getDefault().supportedFileAttributeViews();
            if (setSupportedFileAttributeViews.contains("posix")) {
                filePermissions = new PermissionSupplier() { // from class: com.google.common.io.TempFileCreator$JavaNioCreator$$ExternalSyntheticLambda0
                    @Override // com.google.common.io.TempFileCreator.JavaNioCreator.PermissionSupplier
                    public final FileAttribute get() {
                        return PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));
                    }
                };
                directoryPermissions = new PermissionSupplier() { // from class: com.google.common.io.TempFileCreator$JavaNioCreator$$ExternalSyntheticLambda1
                    @Override // com.google.common.io.TempFileCreator.JavaNioCreator.PermissionSupplier
                    public final FileAttribute get() {
                        return PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
                    }
                };
            } else if (setSupportedFileAttributeViews.contains("acl")) {
                PermissionSupplier permissionSupplierUserPermissions = userPermissions();
                directoryPermissions = permissionSupplierUserPermissions;
                filePermissions = permissionSupplierUserPermissions;
            } else {
                PermissionSupplier permissionSupplier = new PermissionSupplier() { // from class: com.google.common.io.TempFileCreator$JavaNioCreator$$ExternalSyntheticLambda2
                    @Override // com.google.common.io.TempFileCreator.JavaNioCreator.PermissionSupplier
                    public final FileAttribute get() {
                        return TempFileCreator.JavaNioCreator.lambda$static$2();
                    }
                };
                directoryPermissions = permissionSupplier;
                filePermissions = permissionSupplier;
            }
        }

        private JavaNioCreator() {
            super();
        }

        private static String getUsername() {
            String str = (String) Objects.requireNonNull(StandardSystemProperty.USER_NAME.value());
            try {
                Class<?> cls = Class.forName("java.lang.ProcessHandle");
                Class<?> cls2 = Class.forName("java.lang.ProcessHandle$Info");
                Class<?> cls3 = Class.forName("java.util.Optional");
                Method method = cls.getMethod("current", new Class[0]);
                Method method2 = cls.getMethod("info", new Class[0]);
                return (String) Objects.requireNonNull(cls3.getMethod("orElse", Object.class).invoke(cls2.getMethod("user", new Class[0]).invoke(method2.invoke(method.invoke(null, new Object[0]), new Object[0]), new Object[0]), str));
            } catch (ClassNotFoundException e) {
                return str;
            } catch (IllegalAccessException e2) {
                return str;
            } catch (NoSuchMethodException e3) {
                return str;
            } catch (InvocationTargetException e4) {
                Throwables.throwIfUnchecked(e4.getCause());
                return str;
            }
        }

        static /* synthetic */ FileAttribute lambda$static$2() throws IOException {
            throw new IOException("unrecognized FileSystem type " + FileSystems.getDefault());
        }

        static /* synthetic */ FileAttribute lambda$userPermissions$3(FileAttribute fileAttribute) throws IOException {
            return fileAttribute;
        }

        static /* synthetic */ FileAttribute lambda$userPermissions$4(IOException iOException) throws IOException {
            throw new IOException("Could not find user", iOException);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static PermissionSupplier userPermissions() {
            try {
                final FileAttribute<ImmutableList<AclEntry>> fileAttribute = new FileAttribute<ImmutableList<AclEntry>>(ImmutableList.of(AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(FileSystems.getDefault().getUserPrincipalLookupService().lookupPrincipalByName("root")).setPermissions(EnumSet.allOf(AclEntryPermission.class)).setFlags(AclEntryFlag.DIRECTORY_INHERIT, AclEntryFlag.FILE_INHERIT).build())) { // from class: com.google.common.io.TempFileCreator.JavaNioCreator.1
                    final ImmutableList val$acl;

                    {
                        this.val$acl = immutableList;
                    }

                    @Override // java.nio.file.attribute.FileAttribute
                    public String name() {
                        return "acl:acl";
                    }

                    /* JADX WARN: Can't rename method to resolve collision */
                    @Override // java.nio.file.attribute.FileAttribute
                    public ImmutableList<AclEntry> value() {
                        return this.val$acl;
                    }
                };
                return new PermissionSupplier(fileAttribute) { // from class: com.google.common.io.TempFileCreator$JavaNioCreator$$ExternalSyntheticLambda3
                    public final FileAttribute f$0;

                    {
                        this.f$0 = fileAttribute;
                    }

                    @Override // com.google.common.io.TempFileCreator.JavaNioCreator.PermissionSupplier
                    public final FileAttribute get() {
                        return TempFileCreator.JavaNioCreator.lambda$userPermissions$3(this.f$0);
                    }
                };
            } catch (IOException e) {
                return new PermissionSupplier(e) { // from class: com.google.common.io.TempFileCreator$JavaNioCreator$$ExternalSyntheticLambda4
                    public final IOException f$0;

                    {
                        this.f$0 = e;
                    }

                    @Override // com.google.common.io.TempFileCreator.JavaNioCreator.PermissionSupplier
                    public final FileAttribute get() {
                        return TempFileCreator.JavaNioCreator.lambda$userPermissions$4(this.f$0);
                    }
                };
            }
        }

        @Override // com.google.common.io.TempFileCreator
        File createTempDir() {
            try {
                return java.nio.file.Files.createTempDirectory(Paths.get(StandardSystemProperty.JAVA_IO_TMPDIR.value(), new String[0]), null, directoryPermissions.get()).toFile();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create directory", e);
            }
        }

        @Override // com.google.common.io.TempFileCreator
        File createTempFile(String str) throws IOException {
            return java.nio.file.Files.createTempFile(Paths.get(StandardSystemProperty.JAVA_IO_TMPDIR.value(), new String[0]), str, null, filePermissions.get()).toFile();
        }
    }

    private static final class ThrowingCreator extends TempFileCreator {
        private static final String MESSAGE = "Guava cannot securely create temporary files or directories under SDK versions before Jelly Bean. You can create one yourself, either in the insecure default directory or in a more secure directory, such as context.getCacheDir(). For more information, see the Javadoc for Files.createTempDir().";

        private ThrowingCreator() {
            super();
        }

        @Override // com.google.common.io.TempFileCreator
        File createTempDir() {
            throw new IllegalStateException(MESSAGE);
        }

        @Override // com.google.common.io.TempFileCreator
        File createTempFile(String str) throws IOException {
            throw new IOException(MESSAGE);
        }
    }

    private TempFileCreator() {
    }

    private static TempFileCreator pickSecureCreator() {
        try {
            Class.forName("java.nio.file.Path");
            return new JavaNioCreator();
        } catch (ClassNotFoundException e) {
            try {
                return ((Integer) Class.forName("android.os.Build$VERSION").getField("SDK_INT").get(null)).intValue() < ((Integer) Class.forName("android.os.Build$VERSION_CODES").getField("JELLY_BEAN").get(null)).intValue() ? new ThrowingCreator() : new JavaIoCreator();
            } catch (ClassNotFoundException e2) {
                return new ThrowingCreator();
            } catch (IllegalAccessException e3) {
                return new ThrowingCreator();
            } catch (NoSuchFieldException e4) {
                return new ThrowingCreator();
            }
        }
    }

    static void testMakingUserPermissionsFromScratch() throws IOException {
        JavaNioCreator.userPermissions().get();
    }

    abstract File createTempDir();

    abstract File createTempFile(String str) throws IOException;
}
