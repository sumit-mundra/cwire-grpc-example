package sumitm.grpc.examples;

import com.google.gson.Gson;
import io.grpc.MethodDescriptor;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


public class GsonMarshaller<T> implements MethodDescriptor.Marshaller<T> {

    private final Class<T> clazz;

    private final Gson gson = new Gson();

    public GsonMarshaller(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public InputStream stream(T value) {
        return new ByteArrayInputStream(gson.toJson(value).getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public T parse(InputStream stream) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();
        reader.lines().sequential().forEach(
                sb::append
        );
        return gson.fromJson(sb.toString(), this.clazz);
    }

}
