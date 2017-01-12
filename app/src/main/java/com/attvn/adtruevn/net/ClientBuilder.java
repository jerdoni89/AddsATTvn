package com.attvn.adtruevn.net;

import com.attvn.adtruevn.util.Params;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.realm.RealmObject;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by app on 1/5/17.
 * Singleton class.
 */

public final class ClientBuilder {

    private ClientBuilder() {
    }

    private static class Holder {
        private static final ClientBuilder instance = new ClientBuilder();
    }

    public static ClientBuilder getInstance() {
        return Holder.instance;
    }

    private OkHttpClient createClient() {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.interceptors().add(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                Request.Builder requestBuilder = original.newBuilder()
                        .method(original.method(), original.body());

                Request request = requestBuilder.build();
                return chain.proceed(request);
            }
        });
        return httpClient.build();
    }

    private OkHttpClient createClientWithContentTypeJson() {
        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.interceptors().add(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                // Request customization: add request headers
                Request.Builder requestBuilder = original.newBuilder()
                        .addHeader("Content-Type", "application/json")
                        .method(original.method(), original.body());

                Request request = requestBuilder.build();
                return chain.proceed(request);
            }
        });

        return httpClient.build();
    }

    private OkHttpClient createClientWithCookieHeader(final String value) {
        if (value == null || value.isEmpty()) return createClient();

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.interceptors().add(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                // Request customization: add request headers
                Request.Builder requestBuilder = original.newBuilder()
                        .addHeader("Cookie", value)
                        .method(original.method(), original.body());

                Request request = requestBuilder.build();
                return chain.proceed(request);
            }
        });

        return httpClient.build();
    }

    private OkHttpClient createClientWithHeaders(final HashMap<String, String> mapHeaders) {
        if (mapHeaders.isEmpty()) return createClient();

        OkHttpClient.Builder httpClient = new OkHttpClient.Builder();
        httpClient.interceptors().add(new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request original = chain.request();

                // Request customization: add request headers
                Request.Builder requestBuilder = original.newBuilder();
                for (Map.Entry<String, String> entry : mapHeaders.entrySet()) {
                    requestBuilder.addHeader(entry.getKey(), entry.getValue());
                }
                requestBuilder.method(original.method(), original.body());

                Request request = requestBuilder.build();
                return chain.proceed(request);
            }
        });
        return httpClient.build();
    }

    private Gson createCustomGsonForRealmObject() {
        return new GsonBuilder()
                .setExclusionStrategies(new ExclusionStrategy() {
                    @Override
                    public boolean shouldSkipField(FieldAttributes f) {
                        return f.getDeclaringClass().equals(RealmObject.class);
                    }

                    @Override
                    public boolean shouldSkipClass(Class<?> clazz) {
                        return false;
                    }
                })
                .create();
    }

    public WebService createDefaultWebService() {
        return new Retrofit.Builder()
                .baseUrl(Params.BASE_API_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(createClient())
                .build()
                .create(WebService.class);
    }

    public WebService createApplicationWebService() {
        return new Retrofit.Builder()
                .baseUrl(Params.BASE_API_URL)
                .addConverterFactory(GsonConverterFactory.create(createCustomGsonForRealmObject()))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(createClient())
                .build()
                .create(WebService.class);
    }

    public WebService createApplicationWebServiceWithJsonContentType() {
        return new Retrofit.Builder()
                .baseUrl(Params.BASE_API_URL)
                .addConverterFactory(GsonConverterFactory.create(createCustomGsonForRealmObject()))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(createClientWithContentTypeJson())
                .build()
                .create(WebService.class);
    }

    public WebService createApplicationWebServiceWithCookieHeader(String cookie) {
        return new Retrofit.Builder()
                .baseUrl(Params.BASE_API_URL)
                .addConverterFactory(GsonConverterFactory.create(createCustomGsonForRealmObject()))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(createClientWithCookieHeader(cookie))
                .build()
                .create(WebService.class);
    }

    public WebService createApplicationWebServiceWithHeaders(HashMap<String, String> headers) {
        return new Retrofit.Builder()
                .baseUrl(Params.BASE_API_URL)
                .addConverterFactory(GsonConverterFactory.create(createCustomGsonForRealmObject()))
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(createClientWithHeaders(headers))
                .build()
                .create(WebService.class);
    }
}
