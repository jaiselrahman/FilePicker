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
import android.database.ContentObserver;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;

import com.jaiselrahman.filepicker.config.Configurations;

public class MediaFileViewModel extends ViewModel {
    private ContentResolver contentResolver;
    public LiveData<PagedList<MediaFile>> mediaFiles;

    private ContentObserver contentObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
            refresh();
        }
    };

    private MediaFileViewModel(ContentResolver contentResolver, Configurations configs, Long dirId) {
        this.contentResolver = contentResolver;

        MediaFileDataSource.Factory mediaFileDataSourceFactory = new MediaFileDataSource.Factory(contentResolver, configs, dirId);

        mediaFiles = new LivePagedListBuilder<>(
                mediaFileDataSourceFactory,
                new PagedList.Config.Builder()
                        .setPageSize(Configurations.PAGE_SIZE)
                        .setInitialLoadSizeHint(15)
                        .setMaxSize(Configurations.PAGE_SIZE * 3)
                        .setPrefetchDistance(Configurations.PREFETCH_DISTANCE)
                        .setEnablePlaceholders(false)
                        .build()
        ).build();

        contentResolver.registerContentObserver(mediaFileDataSourceFactory.getUri(), true, contentObserver);
    }

    @Override
    protected void onCleared() {
        contentResolver.unregisterContentObserver(contentObserver);
    }

    public void refresh() {
        if (mediaFiles.getValue() != null)
            mediaFiles.getValue().getDataSource().invalidate();
    }

    public static class Factory extends ViewModelProvider.NewInstanceFactory {
        private ContentResolver contentResolver;
        private Configurations configs;
        private Long dirId;

        public Factory(ContentResolver contentResolver, Configurations configs, Long dirId) {
            this.contentResolver = contentResolver;
            this.configs = configs;
            this.dirId = dirId;
        }

        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new MediaFileViewModel(contentResolver, configs, dirId);
        }
    }
}
