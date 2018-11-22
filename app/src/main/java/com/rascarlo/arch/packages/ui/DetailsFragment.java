/*
 *     Copyright (C) 2018 rascarlo <rascarlo@gmail.com>
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.rascarlo.arch.packages.ui;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rascarlo.arch.packages.R;
import com.rascarlo.arch.packages.adapters.DependencyAdapter;
import com.rascarlo.arch.packages.api.model.Details;
import com.rascarlo.arch.packages.api.model.Files;
import com.rascarlo.arch.packages.callbacks.DependencyAdapterCallback;
import com.rascarlo.arch.packages.callbacks.DetailsFragmentCallback;
import com.rascarlo.arch.packages.databinding.FragmentDetailsBinding;
import com.rascarlo.arch.packages.persistence.RoomFile;
import com.rascarlo.arch.packages.util.ArchPackagesStringConverters;
import com.rascarlo.arch.packages.viewmodel.DetailsViewModel;
import com.rascarlo.arch.packages.viewmodel.DetailsViewModelFactory;
import com.rascarlo.arch.packages.viewmodel.FilesViewModel;
import com.rascarlo.arch.packages.viewmodel.FilesViewModelFactory;
import com.rascarlo.arch.packages.viewmodel.RoomFileViewModel;

import java.util.HashMap;
import java.util.List;

public class DetailsFragment extends Fragment implements DependencyAdapterCallback {

    private static final String BUNDLE_REPO = "bundle_repo";
    private static final String BUNDLE_ARCH = "bundle_arch";
    private static final String BUNDLE_PKGNAME = "bundle_pkgname";
    private String bundleRepo;
    private String bundleArch;
    private String bundlePkgname;
    private Context context;
    private FragmentDetailsBinding fragmentDetailsBinding;
    private DetailsFragmentCallback detailsFragmentCallback;

    public DetailsFragment() {
    }

    public static DetailsFragment newInstance(String repo,
                                              String arch,
                                              String pkgname) {
        DetailsFragment detailsFragment = new DetailsFragment();
        Bundle bundle = new Bundle();
        bundle.putString(BUNDLE_REPO, repo);
        bundle.putString(BUNDLE_ARCH, arch);
        bundle.putString(BUNDLE_PKGNAME, pkgname);
        detailsFragment.setArguments(bundle);
        return detailsFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bundleRepo = getArguments().getString(BUNDLE_REPO);
            bundleArch = getArguments().getString(BUNDLE_ARCH);
            bundlePkgname = getArguments().getString(BUNDLE_PKGNAME);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        if (context instanceof DetailsFragmentCallback) {
            detailsFragmentCallback = (DetailsFragmentCallback) context;
        } else {
            throw new RuntimeException(context.toString()
                    + "must implement DetailsFragmentCallback");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        detailsFragmentCallback = null;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentDetailsBinding = FragmentDetailsBinding.inflate(inflater, container, false);
        return fragmentDetailsBinding.getRoot();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (fragmentDetailsBinding != null) {
            // details
            DetailsViewModelFactory detailsViewModelFactory = new DetailsViewModelFactory(bundleRepo, bundleArch, bundlePkgname);
            DetailsViewModel detailsViewModel = ViewModelProviders.of(this, detailsViewModelFactory).get(DetailsViewModel.class);
            detailsViewModel.getDetailsLiveData().observe(this, details -> {
                if (details != null && fragmentDetailsBinding != null) {
                    fragmentDetailsBinding.setDetails(details);
                    fragmentDetailsBinding.executePendingBindings();
                    bindDetailsViewModel(details);
                }
            });
            // files
            FilesViewModelFactory filesViewModelFactory = new FilesViewModelFactory(bundleRepo, bundleArch, bundlePkgname);
            FilesViewModel filesViewModel = ViewModelProviders.of(this, filesViewModelFactory).get(FilesViewModel.class);
            filesViewModel.getFilesLiveData().observe(this, files -> {
                if (files != null && fragmentDetailsBinding != null) {
                    fragmentDetailsBinding.detailsFilesLayout.setFiles(files);
                    bindFilesViewModel(files);
                }
            });
        }
    }

    @Override
    public void onDependencyAdapterCallbackOnPackageClicked(String packageName) {
        if (detailsFragmentCallback != null) {
            detailsFragmentCallback.onDetailsFragmentCallbackOnPackageClicked(packageName);
        }
    }

    private void bindDetailsViewModel(Details details) {
        if (fragmentDetailsBinding != null && details != null) {
            HashMap<RecyclerView, List<String>> hashMap = new HashMap<>();
            // compressed size
            bindCompressedSize(details);
            // installed size
            bindInstalledSize(details);
            // dependencies
            hashMap.put(fragmentDetailsBinding.detailsDependenciesLayout.detailsDependenciesRecyclerView, details.depends);
            // make dependencies
            hashMap.put(fragmentDetailsBinding.detailsMakeDependenciesLayout.detailsMakeDependenciesRecyclerView, details.makedepends);
            // check dependencies
            hashMap.put(fragmentDetailsBinding.detailsCheckDependenciesLayout.detailsCheckDependenciesRecyclerView, details.checkdepends);
            // opt dependencies
            hashMap.put(fragmentDetailsBinding.detailsOptDependenciesLayout.detailsOptDependenciesRecyclerView, details.optdepends);
            // conflicts
            hashMap.put(fragmentDetailsBinding.detailsConflictsLayout.detailsConflictsRecyclerView, details.conflicts);
            // provides
            hashMap.put(fragmentDetailsBinding.detailsProvidesLayout.detailsProvidesRecyclerView, details.provides);
            // replaces
            hashMap.put(fragmentDetailsBinding.detailsReplacesLayout.detailsReplacesRecyclerView, details.replaces);
            for (HashMap.Entry<RecyclerView, List<String>> entry : hashMap.entrySet()) {
                populateRecyclerView(entry.getKey(), entry.getValue());
            }
        }
    }

    private void bindFilesViewModel(Files files) {
        if (fragmentDetailsBinding != null && files != null) {
            // files
            bindFiles(files);
        }
    }

    private void bindCompressedSize(Details details) {
        if (details.compressedSize != null && !TextUtils.isEmpty(details.compressedSize)) {
            fragmentDetailsBinding.detailsBodyLayout.detailsTextViewCompressedSize
                    .setText(String.format(getString(R.string.formatted_compressed_size),
                            details.compressedSize,
                            ArchPackagesStringConverters.convertBytesToMb(context, details.compressedSize)));
        }
    }

    private void bindInstalledSize(Details details) {
        if (details.installedSize != null && !TextUtils.isEmpty(details.installedSize)) {
            fragmentDetailsBinding.detailsBodyLayout.detailsTextViewInstalledSize
                    .setText(String.format(getString(R.string.formatted_installed_size),
                            details.installedSize,
                            ArchPackagesStringConverters.convertBytesToMb(context, details.installedSize)));
        }
    }

    private void bindFiles(Files files) {
        if (files.files != null) {
            RoomFileViewModel roomFileViewModel = ViewModelProviders.of(this).get(RoomFileViewModel.class);
            roomFileViewModel.wipeRoomFileDatabase();
            for (String s : files.files) {
                roomFileViewModel.insertRoomFile(new RoomFile(s.trim()));
            }
            fragmentDetailsBinding.detailsFilesLayout.detailsFilesButton.setOnClickListener(v -> {
                if (detailsFragmentCallback != null) {
                    detailsFragmentCallback.onDetailsFragmentCallbackOnFilesClicked(files);
                }
            });
        }
    }

    private void populateRecyclerView(RecyclerView recyclerView, List<String> stringList) {
        if (stringList != null && !stringList.isEmpty()) {
            LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.setHasFixedSize(true);
            recyclerView.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL));
            DependencyAdapter dependencyAdapter = new DependencyAdapter(this);
            recyclerView.setAdapter(dependencyAdapter);
            dependencyAdapter.submitList(stringList);
        }
    }
}