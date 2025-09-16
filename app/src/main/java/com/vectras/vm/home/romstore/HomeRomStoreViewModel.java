package com.vectras.vm.home.romstore;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.vectras.vm.Roms.DataRoms;

import java.util.ArrayList;
import java.util.List;

public class HomeRomStoreViewModel extends ViewModel {
    private final MutableLiveData<List<DataRoms>> romsList = new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<DataRoms>> getRomsList() {
        return romsList;
    }

    public void setRomsList(List<DataRoms> data) {
        romsList.setValue(data);
    }
}

