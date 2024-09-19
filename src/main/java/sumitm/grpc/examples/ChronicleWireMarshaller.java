package sumitm.grpc.examples;

import io.grpc.MethodDescriptor;
import net.openhft.chronicle.core.pool.ClassAliasPool;
import net.openhft.chronicle.wire.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static net.openhft.chronicle.wire.WireType.BINARY;

/**
 * This marshaller uses {@link WireToOutputStream} and {@link InputStreamToWire} to marshall and unmarshall objects.
 * The chosen wiretype is {@link WireType#BINARY}
 *
 * @param <T> generic type which extends Marshallable from chronicle wire libs
 */
public class ChronicleWireMarshaller<T extends Marshallable> implements MethodDescriptor.Marshaller<T> {

    private final Class<T> clazz;

    public ChronicleWireMarshaller(Class<T> clazz) {
        this.clazz = clazz;
        ClassAliasPool.CLASS_ALIASES.addAlias(clazz);
    }

    @Override
    public InputStream stream(T value) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        WireToOutputStream wtos = new WireToOutputStream(BINARY, os);
        wtos.getWire().getValueOut().object(value);
        try {
            wtos.flush();
            return new ByteArrayInputStream(os.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public T parse(InputStream stream) {
        InputStreamToWire inputStreamToWire = new InputStreamToWire(WireType.BINARY, stream);
        Wire wire;
        try {
            wire = inputStreamToWire.readOne();
            return wire.getValueIn().object(getMessageClass());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Class<T> getMessageClass() {
        return clazz;
    }
}
