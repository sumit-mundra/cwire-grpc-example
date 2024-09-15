package sumitm.grpc.examples;

import io.grpc.MethodDescriptor;
import net.openhft.chronicle.wire.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static net.openhft.chronicle.wire.WireType.BINARY;

// Your custom serializer class
class ChronicleWireMarshaller<T extends Marshallable> implements MethodDescriptor.Marshaller<T> {

    private final Class<T> clazz;

    public ChronicleWireMarshaller(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public InputStream stream(T value) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        WireToOutputStream wtos = new WireToOutputStream(BINARY, os);
        wtos.getWire().getValueOut().object(value);
        try {
            wtos.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ByteArrayInputStream(os.toByteArray());
    }

    @Override
    public T parse(InputStream stream) {
        InputStreamToWire inputStreamToWire = new InputStreamToWire(WireType.BINARY, stream);
        Wire wire;
        try {
            wire = inputStreamToWire.readOne();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return wire.getValueIn().object(getMessageClass());
    }

    public Class<T> getMessageClass() {
        return clazz;
    }
}
