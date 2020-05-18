/*
 *  Copyright (c) 2020, Jaisel Rahman <jaiselrahman@gmail.com>.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.jaiselrahman.filepicker.model;

import android.content.ContentResolver;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;

import com.jaiselrahman.filepicker.config.Configurations;

public class DirViewModel extends ViewModel {
    public LiveData<PagedList<Dir>> dirs;

    private DirViewModel(ContentResolver contentResolver, Configurations configs) {
        DirDataSource.Factory dirDataSourceFactory = new DirDataSource.Factory(contentResolver, configs);

        dirs = new LivePagedListBuilder<>(
                dirDataSourceFactory,
                new PagedList.Config.Builder()
                        .setPageSize(Configurations.PAGE_SIZE)
                        .setInitialLoadSizeHint(15)
                        .setMaxSize(Configurations.PAGE_SIZE * 3)
                        .setPrefetchDistance(Configurations.PREFETCH_DISTANCE)
                        .setEnablePlaceholders(false)
                        .build()
        ).build();
    }

    public void refresh() {
        if (dirs.getValue() != null)
            dirs.getValue().getDataSource().invalidate();
    }

    public static class Factory extends ViewModelProvider.NewInstanceFactory {
        private ContentResolver contentResolver;
        private Configurations configs;

        public Factory(ContentResolver contentResolver, Configurations configs) {
            this.contentResolver = contentResolver;
            this.configs = configs;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new DirViewModel(contentResolver, configs);
        }
    }
}
