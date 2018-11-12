package com.rascarlo.arch.packages.data;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;

import com.rascarlo.arch.packages.api.ArchPackagesService;
import com.rascarlo.arch.packages.api.model.Details;
import com.rascarlo.arch.packages.api.model.Files;
import com.rascarlo.arch.packages.api.model.Packages;
import com.rascarlo.arch.packages.util.ArchPackagesConstants;

import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ArchPackagesRepository {

    private final ArchPackagesService archPackagesService;
    private static ArchPackagesRepository archPackagesRepository;

    private ArchPackagesRepository() {
        // http logging interceptor
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        // okhttp client
        OkHttpClient.Builder okHttpClient = new OkHttpClient.Builder();
        okHttpClient.addInterceptor(httpLoggingInterceptor);
        // retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ArchPackagesConstants.ARCH_PACKAGES_API_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient.build())
                .build();
        // service
        archPackagesService = retrofit.create(ArchPackagesService.class);
    }

    public synchronized static ArchPackagesRepository getArchPackagesRepositoryInstance() {
        if (archPackagesRepository == null) {
            archPackagesRepository = new ArchPackagesRepository();
        }
        return archPackagesRepository;
    }

    public LiveData<Packages> getPackagesLiveData(int keywordsParameter,
                                                  String keywords,
                                                  List<String> listRepo,
                                                  List<String> listArch,
                                                  String flagged,
                                                  int numPage) {
        final MutableLiveData<Packages> packagesMutableLiveData = new MutableLiveData<>();
        Call<Packages> archPackagesCall;
        // by name or description
        if (keywordsParameter == ArchPackagesConstants.SEARCH_KEYWORDS_PARAMETER_NAME_OR_DESCRIPTION) {
            archPackagesCall = archPackagesService.searchByNameOrDescription(keywords, listRepo, listArch, flagged, numPage);
            // by exact name
        } else if (keywordsParameter == ArchPackagesConstants.SEARCH_KEYWORDS_PARAMETER_EXACT_NAME) {
            archPackagesCall = archPackagesService.searchByExactName(keywords, listRepo, listArch, flagged, numPage);
            // by description
        } else if (keywordsParameter == ArchPackagesConstants.SEARCH_KEYWORDS_PARAMETER_DESCRIPTION) {
            archPackagesCall = archPackagesService.searchByDescription(keywords, listRepo, listArch, flagged, numPage);
            // DEFAULT by name or description
        } else {
            archPackagesCall = archPackagesService.searchByNameOrDescription(keywords, listRepo, listArch, flagged, numPage);
        }
        archPackagesCall.enqueue(new Callback<Packages>() {
            @Override
            public void onResponse(Call<Packages> call, Response<Packages> response) {
                if (response.isSuccessful() && response.body() != null && response.code() == 200)
                    packagesMutableLiveData.setValue(response.body());
                else packagesMutableLiveData.setValue(null);
            }

            @Override
            public void onFailure(Call<Packages> call, Throwable t) {
                packagesMutableLiveData.setValue(null);
            }
        });
        return packagesMutableLiveData;
    }

    public LiveData<Details> getDetailsLiveData(final String repo,
                                                String arch,
                                                String pkgname) {
        final MutableLiveData<Details> detailsMutableLiveData = new MutableLiveData<>();
        Call<Details> detailsCall = archPackagesService.searchDetails(repo, arch, pkgname);
        detailsCall.enqueue(new Callback<Details>() {
            @Override
            public void onResponse(Call<Details> call, Response<Details> response) {
                if (response.isSuccessful() && response.body() != null && response.code() == 200)
                    detailsMutableLiveData.setValue(response.body());
                else detailsMutableLiveData.setValue(null);
            }

            @Override
            public void onFailure(Call<Details> call, Throwable t) {
                detailsMutableLiveData.setValue(null);
            }
        });
        return detailsMutableLiveData;
    }

    public LiveData<Files> getFilesLiveData(final String repo,
                                            String arch,
                                            String pkgname) {
        final MutableLiveData<Files> filesMutableLiveData = new MutableLiveData<>();
        Call<Files> filesCall = archPackagesService.searchFiles(repo, arch, pkgname);
        filesCall.enqueue(new Callback<Files>() {
            @Override
            public void onResponse(Call<Files> call, Response<Files> response) {
                if (response.isSuccessful() && response.body() != null && response.code() == 200)
                    filesMutableLiveData.setValue(response.body());
                else filesMutableLiveData.setValue(null);
            }

            @Override
            public void onFailure(Call<Files> call, Throwable t) {
                filesMutableLiveData.setValue(null);
            }
        });
        return filesMutableLiveData;
    }
}
