package org.thoughtcrime.securesms.lock.v2;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.core.util.Preconditions;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.thoughtcrime.securesms.lock.v2.ConfirmKbsPinRepository.PinSetResult;

final class ConfirmKbsPinViewModel extends ViewModel implements BaseKbsPinViewModel {

  private final ConfirmKbsPinRepository repository;

  private final MutableLiveData<KbsPin>          userEntry     = new MutableLiveData<>(KbsPin.EMPTY);
  private final MutableLiveData<KbsKeyboardType> keyboard      = new MutableLiveData<>(KbsKeyboardType.NUMERIC);
  private final MutableLiveData<SaveAnimation>   saveAnimation = new MutableLiveData<>(SaveAnimation.NONE);
  private final MutableLiveData<Label>           label         = new MutableLiveData<>(Label.RE_ENTER_PIN);

  private final KbsPin pinToConfirm;

  private ConfirmKbsPinViewModel(@NonNull KbsPin pinToConfirm,
                                 @NonNull KbsKeyboardType keyboard,
                                 @NonNull ConfirmKbsPinRepository repository)
  {
    this.keyboard.setValue(keyboard);

    this.pinToConfirm = pinToConfirm;
    this.repository   = repository;
  }

  LiveData<SaveAnimation> getSaveAnimation() {
    return Transformations.distinctUntilChanged(saveAnimation);
  }

  LiveData<Label> getLabel() {
    return Transformations.distinctUntilChanged(label);
  }

  @Override
  public void confirm() {
    KbsPin userEntry = this.userEntry.getValue();
    this.userEntry.setValue(KbsPin.EMPTY);

    if (pinToConfirm.toString().equals(userEntry.toString())) {
      this.label.setValue(Label.CREATING_PIN);
      this.saveAnimation.setValue(SaveAnimation.LOADING);

      repository.setPin(pinToConfirm, Preconditions.checkNotNull(this.keyboard.getValue()), this::handleResult);
    } else {
      this.label.setValue(Label.PIN_DOES_NOT_MATCH);
    }
  }

  void onLoadingAnimationComplete() {
    this.label.setValue(Label.EMPTY);
  }

  @Override
  public LiveData<KbsPin> getUserEntry() {
    return userEntry;
  }

  @Override
  public LiveData<KbsKeyboardType> getKeyboard() {
    return keyboard;
  }

  @MainThread
  public void setUserEntry(String userEntry) {
    this.userEntry.setValue(KbsPin.from(userEntry));
  }

  @MainThread
  public void toggleAlphaNumeric() {
    this.keyboard.setValue(this.keyboard.getValue().getOther());
  }

  private void handleResult(PinSetResult result) {
    switch (result) {
      case SUCCESS:
        this.saveAnimation.setValue(SaveAnimation.SUCCESS);
        break;
      case FAILURE:
        this.saveAnimation.setValue(SaveAnimation.FAILURE);
        break;
      default:
        throw new IllegalStateException("Unknown state: " + result.name());
    }
  }

  enum Label {
    RE_ENTER_PIN,
    PIN_DOES_NOT_MATCH,
    CREATING_PIN,
    EMPTY
  }

  enum SaveAnimation {
    NONE,
    LOADING,
    SUCCESS,
    FAILURE
  }

  static final class Factory implements ViewModelProvider.Factory {

    private final KbsPin                  pinToConfirm;
    private final KbsKeyboardType         keyboard;
    private final ConfirmKbsPinRepository repository;

    Factory(@NonNull KbsPin pinToConfirm,
            @NonNull KbsKeyboardType keyboard,
            @NonNull ConfirmKbsPinRepository repository)
    {
      this.pinToConfirm = pinToConfirm;
      this.keyboard     = keyboard;
      this.repository   = repository;
    }

    @Override
    public @NonNull <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      //noinspection unchecked
      return (T) new ConfirmKbsPinViewModel(pinToConfirm, keyboard, repository);
    }
  }
}