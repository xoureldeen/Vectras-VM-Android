package com.vectras.vm.main.core;

public class CallbackInterface {
    //Fix Cyclic.
    public interface HomeCallToVmsListener {
        void refeshVMList();
        void configurationChanged(boolean isLandscape);
    }
}
