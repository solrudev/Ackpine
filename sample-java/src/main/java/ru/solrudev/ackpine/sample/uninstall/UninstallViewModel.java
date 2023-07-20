package ru.solrudev.ackpine.sample.uninstall;

import static androidx.lifecycle.SavedStateHandleSupport.createSavedStateHandle;
import static androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.viewmodel.ViewModelInitializer;

import ru.solrudev.ackpine.DisposableSubscriptionContainer;
import ru.solrudev.ackpine.uninstaller.PackageUninstaller;

public class UninstallViewModel extends ViewModel {

    private final MutableLiveData<String> _text = new MutableLiveData<>();
    private final SavedStateHandle _savedStateHandle;
    private final PackageUninstaller _packageUninstaller;
    private final DisposableSubscriptionContainer _subscriptions = new DisposableSubscriptionContainer();

    public UninstallViewModel(PackageUninstaller packageUninstaller, SavedStateHandle savedStateHandle) {
        _packageUninstaller = packageUninstaller;
        _savedStateHandle = savedStateHandle;
        _text.setValue("This is uninstall fragment");
    }

    @Override
    protected void onCleared() {
        _subscriptions.clear();
    }

    public LiveData<String> getText() {
        return _text;
    }

    static final ViewModelInitializer<UninstallViewModel> initializer = new ViewModelInitializer<>(
            UninstallViewModel.class,
            creationExtras -> {
                Application application = creationExtras.get(APPLICATION_KEY);
                assert application != null;
                PackageUninstaller packageUninstaller = PackageUninstaller.getInstance(application);
                SavedStateHandle savedStateHandle = createSavedStateHandle(creationExtras);
                return new UninstallViewModel(packageUninstaller, savedStateHandle);
            }
    );
}