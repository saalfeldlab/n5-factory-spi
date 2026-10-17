package org.janelia.saalfeldlab.n5.universe.factory.spi;

import com.google.gson.GsonBuilder;
import org.janelia.saalfeldlab.n5.KeyValueRoot;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Linkage guard: every class of this artifact must load with no cloud SDK and no n5-hdf5 on
 * the classpath.
 * <p>
 * The SPI is allowed to name {@code S3Client}, {@code S3ClientBuilder}, {@code Storage} and
 * {@code StorageOptions.Builder} only as generic parameters, which erase to {@code Function},
 * {@code Consumer} and {@code Supplier}. An SDK type anywhere in a method descriptor or body
 * makes the class unloadable without the SDK. The build cannot catch that: the SDKs are
 * {@code provided} dependencies, so they are on the compile and test classpath, and imports
 * leave no trace in the class file. Hence this test.
 * <p>
 * The classes are loaded through a fresh class loader that sees only this artifact's own
 * classes, n5 and gson, so the SDKs are absent there although they are present in this JVM.
 */
public class LinkageTest {

	private static final String PACKAGE = LinkageTest.class.getPackage().getName();

	/** Where the class {@code c} was loaded from: a jar, or a classes directory. */
	private static URL locationOf(final Class<?> c) {

		return c.getProtectionDomain().getCodeSource().getLocation();
	}

	/** This artifact's main classes, n5 and gson, and nothing else beyond the JDK. */
	private static URLClassLoader spiOnlyClassLoader() {

		final URL[] urls = {
				locationOf(HDF5Options.class),
				locationOf(KeyValueRoot.class),
				locationOf(GsonBuilder.class)};
		return new URLClassLoader(urls, ClassLoader.getPlatformClassLoader());
	}

	/** Names of all top-level and nested classes in this artifact's package. */
	private static List<String> spiClassNames() throws URISyntaxException {

		final File dir = new File(new File(locationOf(HDF5Options.class).toURI()), PACKAGE.replace('.', '/'));
		final File[] files = dir.listFiles((d, name) -> name.endsWith(".class"));
		assertTrue("no classes found in " + dir, files != null && files.length > 0);

		final List<String> names = new ArrayList<>();
		for (final File file : files) {
			final String name = file.getName();
			names.add(PACKAGE + "." + name.substring(0, name.length() - ".class".length()));
		}
		return names;
	}

	/** Makes sure the loader really hides what the test claims it hides. */
	@Test
	public void sdksAreAbsent() throws Exception {

		try (final URLClassLoader loader = spiOnlyClassLoader()) {
			for (final String sdkClass : new String[]{
					"software.amazon.awssdk.services.s3.S3Client",
					"software.amazon.awssdk.services.s3.S3ClientBuilder",
					"com.google.cloud.storage.Storage",
					"com.google.cloud.storage.StorageOptions",
					"org.janelia.saalfeldlab.n5.s3.AmazonS3KeyValueRoot",
					"org.janelia.saalfeldlab.n5.googlecloud.GoogleCloudStorageKeyValueRoot",
					"org.janelia.saalfeldlab.n5.hdf5.N5HDF5Reader"}) {
				try {
					Class.forName(sdkClass, false, loader);
					fail(sdkClass + " is visible to the SPI-only class loader");
				} catch (final ClassNotFoundException expected) {}
			}
		}
	}

	@Test
	public void spiClassesLoadWithoutSdks() throws Exception {

		final List<String> names = spiClassNames();
		final List<String> failures = new ArrayList<>();
		try (final URLClassLoader loader = spiOnlyClassLoader()) {
			for (final String name : names) {
				try {
					/* initialize = true links and verifies the class */
					final Class<?> c = Class.forName(name, true, loader);
					assertSame(name + " was not loaded by the SPI-only class loader",
							loader, c.getClassLoader());
					/* resolve every descriptor, including those of methods nobody calls */
					c.getDeclaredMethods();
					c.getDeclaredConstructors();
					c.getDeclaredFields();
				} catch (final Throwable t) {
					failures.add(name + ": " + t);
				}
			}
		}
		if (!failures.isEmpty())
			fail("SPI classes that do not load without the cloud SDKs / n5-hdf5:\n  "
					+ String.join("\n  ", failures));
	}
}
