package com.excellence.retrofit;

import android.text.TextUtils;

import com.excellence.retrofit.interfaces.IListener;
import com.excellence.retrofit.utils.Utils;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import static com.excellence.retrofit.utils.Utils.checkHeaders;
import static com.excellence.retrofit.utils.Utils.checkParams;
import static com.excellence.retrofit.utils.Utils.checkURL;
import static java.net.HttpURLConnection.HTTP_OK;

/**
 * <pre>
 *     author : VeiZhang
 *     blog   : http://tiimor.cn
 *     date   : 2017/10/19
 *     desc   : 网络请求创建工具
 * </pre>
 */

public class HttpRequest
{
	public static final String TAG = HttpRequest.class.getSimpleName();

	private RetrofitClient mRetrofitClient = null;
	private Object mTag = null;
	private String mUrl = null;
	private Map<String, String> mHeaders = null;
	private Map<String, String> mParams = null;
	private IListener mListener = null;

	protected HttpRequest(Builder builder)
	{
		mTag = builder.mTag;
		mUrl = builder.mUrl;
		mHeaders = builder.mHeaders;
		mParams = builder.mParams;
		mListener = builder.mListener;

		mRetrofitClient = RetrofitClient.getInstance();
	}

	public static class Builder
	{
		private Object mTag = null;
		private String mUrl = null;
		private Map<String, String> mHeaders = new HashMap<>();
		private Map<String, String> mParams = new HashMap<>();
		private IListener mListener = null;

		/**
		 * 设置网络请求标识，用于取消请求
		 *
		 * @param tag
		 * @return
		 */
		public Builder tag(Object tag)
		{
			mTag = tag;
			return this;
		}

		/**
		 * 请求地址
		 *
		 * @param url
		 * @return
		 */
		public Builder url(String url)
		{
			mUrl = url;
			return this;
		}

		/**
		 * 设置单个请求的请求头
		 *
		 * @param key 键
		 * @param value 键值
		 * @return
		 */
		public Builder header(String key, String value)
		{
			mHeaders.put(key, value);
			return this;
		}

		/**
		 * 设置单个请求的头集合
		 *
		 * @param headers 集合
		 * @return
		 */
		public Builder headers(Map<String, String> headers)
		{
			mHeaders.putAll(headers);
			return this;
		}

		/**
		 * 设置单个请求的参数
		 *
		 * @param key 键
		 * @param value 键值
		 * @return
		 */
		public Builder param(String key, String value)
		{
			mParams.put(key, value);
			return this;
		}

		/**
		 * 设置单个请求的参数集合
		 *
		 * @param params 参数集
		 * @return
		 */
		public Builder params(Map<String, String> params)
		{
			mParams.putAll(params);
			return this;
		}

		public Builder listener(IListener listener)
		{
			mListener = listener;
			return this;
		}

		public HttpRequest build()
		{
			return new HttpRequest(this);
		}
	}

	/**
	 * Get请求字符串数据
	 */
	public void get()
	{
		addRequestInfo();
		Call<String> call = mRetrofitClient.getService().get(checkURL(mUrl), checkParams(mParams), checkHeaders(mHeaders));
		mRetrofitClient.addCall(mTag, mUrl, call);
		call.enqueue(new Callback<String>()
		{
			@Override
			public void onResponse(Call<String> call, Response<String> response)
			{
				if (response.code() == HTTP_OK)
				{
					handleSuccess(mListener, response.body());
				}
				else
				{
					String errorMsg = Utils.inputStream2String(response.errorBody().byteStream());
					if (!TextUtils.isEmpty(errorMsg))
						handleError(mListener, new Throwable(errorMsg));
					else
					{
						// 离线时使用缓存出现异常，如果没有上次缓存，出现异常时是没有打印信息的，添加自定义异常信息方便识别
						handleError(mListener, new Throwable("There may be no cache data!"));
					}
				}
				mRetrofitClient.removeCall(mTag, mUrl);
			}

			@Override
			public void onFailure(Call<String> call, Throwable t)
			{
				if (!call.isCanceled())
				{
					handleError(mListener, t);
				}
				mRetrofitClient.removeCall(mTag, mUrl);
			}
		});
	}

	/**
	 * RxJava结合Get请求字符串数据
	 */
	public void obGet()
	{
		addRequestInfo();
		Observable<String> observable = mRetrofitClient.getService().obGet(checkURL(mUrl), checkParams(mParams), checkHeaders(mHeaders));
		Subscription subscription = observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(new Subscriber<String>()
		{
			@Override
			public void onNext(String result)
			{
				handleSuccess(mListener, result);
				mRetrofitClient.removeCall(mTag, mUrl);
			}

			@Override
			public void onCompleted()
			{

			}

			@Override
			public void onError(Throwable e)
			{
				handleError(mListener, e);
				mRetrofitClient.removeCall(mTag, mUrl);
			}
		});
		mRetrofitClient.addCall(mTag, mUrl, subscription);
	}

	/**
	 * 单个请求的头和参数覆盖全局请求的头和参数
	 */
	private void addRequestInfo()
	{
		Map<String, String> headers = new HashMap<>(mRetrofitClient.getHeaders());
		Map<String, String> params = new HashMap<>(mRetrofitClient.getParams());
		headers.putAll(mHeaders);
		params.putAll(mParams);
		mHeaders = headers;
		mParams = params;
	}

	private void handleSuccess(IListener listener, String result)
	{
		if (listener != null)
			listener.onSuccess(result);
	}

	private void handleError(IListener listener, Throwable t)
	{
		if (listener != null)
			listener.onError(t);
	}
}