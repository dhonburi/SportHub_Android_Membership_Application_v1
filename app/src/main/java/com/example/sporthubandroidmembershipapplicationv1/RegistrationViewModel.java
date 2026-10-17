package com.example.sporthubandroidmembershipapplicationv1;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.sporthubandroidmembershipapplicationv1.models.RegisterRequest;
import com.example.sporthubandroidmembershipapplicationv1.models.RegisterResponse;
import com.example.sporthubandroidmembershipapplicationv1.network.ApiClient;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Converter;
import retrofit2.Response;

public class RegistrationViewModel extends ViewModel {
    public static final class State {
        public final boolean loading;
        public final boolean success;
        public final String message;
        public final Map<String, String[]> errors;

        private State(boolean loading, boolean success, String message,
                      Map<String, String[]> errors) {
            this.loading = loading;
            this.success = success;
            this.message = message;
            this.errors = errors == null ? Collections.emptyMap() : errors;
        }
    }

    private final MutableLiveData<State> state = new MutableLiveData<>(
            new State(false, false, "", null));

    private Call<RegisterResponse> activeCall;
    private String submittedEmail = "";

    public LiveData<State> getState() {
        return state;
    }

    public boolean isLoading() {
        State current = state.getValue();
        return current != null && current.loading;
    }

    public String getSubmittedEmail() {
        return submittedEmail;
    }

    public void submit(RegisterRequest request) {
        if (isLoading()) {
            return;
        }

        submittedEmail = request.getEmail();
        state.setValue(new State(true, false, "", null));

        activeCall = ApiClient.getAuthApiService().register(request);
        activeCall.enqueue(new Callback<RegisterResponse>() {
            @Override
            public void onResponse(Call<RegisterResponse> call,
                                   Response<RegisterResponse> response) {
                if (call.isCanceled()) {
                    return;
                }

                activeCall = null;

                RegisterResponse body = response.isSuccessful()
                        ? response.body() : readError(response.errorBody());

                if (response.isSuccessful() && body != null && body.isSuccess()
                        && body.getMemberId() != null && body.getMemberId() > 0
                        && body.getMemberNumber() != null
                        && !body.getMemberNumber().trim().isEmpty()) {
                    state.setValue(new State(false, true,
                            "Account created successfully. You can now sign in.", null));
                    return;
                }

                String message;

                if (response.code() == 409) {
                    message = "An account already uses this email. "
                            + "If you already submitted this form, try signing in.";
                } else if (response.code() == 400) {
                    message = "Please correct the highlighted details and try again.";
                } else if (response.code() == 404 || response.code() == 405) {
                    message = "Account registration is not available on this server. "
                            + "Check that the updated API has been deployed.";
                } else if (response.code() == 429) {
                    message = "Too many requests. Please wait before trying again.";
                } else {
                    message = uncertainMessage();
                }

                state.setValue(new State(false, false, message,
                        body == null ? null : body.getErrors()));
            }

            @Override
            public void onFailure(Call<RegisterResponse> call, Throwable error) {
                if (call.isCanceled()) {
                    return;
                }

                activeCall = null;
                state.setValue(new State(false, false, uncertainMessage(), null));
            }
        });
    }

    private RegisterResponse readError(ResponseBody errorBody) {
        if (errorBody == null) {
            return null;
        }

        try (ResponseBody body = errorBody) {
            Converter<ResponseBody, RegisterResponse> converter =
                    ApiClient.getClient().responseBodyConverter(
                            RegisterResponse.class, new Annotation[0]);

            return converter.convert(body);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String uncertainMessage() {
        return "We could not confirm whether your account was created. "
                + "Check your connection, then try signing in if you already submitted. "
                + "You can also resubmit; the same email cannot create duplicate accounts.";
    }

    @Override
    protected void onCleared() {
        if (activeCall != null) {
            activeCall.cancel();
            activeCall = null;
        }

        super.onCleared();
    }
}