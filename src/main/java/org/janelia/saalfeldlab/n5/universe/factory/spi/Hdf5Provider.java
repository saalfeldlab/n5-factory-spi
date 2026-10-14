package org.janelia.saalfeldlab.n5.universe.factory.spi;

import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.N5Writer;

/**
 * The HDF5 format: knows how to open an HDF5 container given a path and
 * {@link HDF5Options}.
 * <p>
 * This is the service interface of the {@code n5-hdf5} module.
 */
public interface Hdf5Provider {

	/**
	 * Open an HDF5 container for reading.
	 *
	 * @param containerPath path of the HDF5 file
	 * @param options the format configuration
	 * @return the reader
	 */
	N5Reader buildReader(String containerPath, HDF5Options options);

	/**
	 * Open an HDF5 container for writing, creating it if it does not exist.
	 *
	 * @param containerPath path of the HDF5 file
	 * @param options the format configuration
	 * @return the writer
	 */
	N5Writer buildWriter(String containerPath, HDF5Options options);
}
