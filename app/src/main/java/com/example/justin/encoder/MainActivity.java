package com.example.justin.encoder;

import android.app.Activity;
import android.os.Bundle;
import android.util.ArrayMap;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Objects;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;


public class MainActivity extends Activity {
    private static final String TAG = "shit";

    private TextView textView;
    private EditText editText;
    private EditText editText2;

    private String myMD5;

    private int showLength;

    private Map<Integer, String> numMap;

    private boolean clickable1 = true;
    private boolean clickable2 = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = this.findViewById(R.id.out);
        editText = this.findViewById(R.id.text);
        editText2 = this.findViewById(R.id.salt);
        final Button button = this.findViewById(R.id.button);

        final RadioGroup radioGroup1 = this.findViewById(R.id.group1);
        final RadioGroup radioGroup2 = this.findViewById(R.id.group2);

        final Observer<String> observer = new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {
                //先把32位的md5数字算出来
                computer();
                //再32位数据进行拆分，放进map
                dataPutMap(myMD5);
            }

            @Override
            public void onNext(String s) {
                /**
                 *  ###############  处理RadioGroup 冲突问题
                 * 这套机制最重要，当点击一个radioGroup的button时，另外的group要清空
                 * 这时候盲目使用radioGroup.clearCheck()是不行的，因为我使用了被观察者，
                 * 每运行一次clearCheck都会访问一次监听器，被观察者也会被运行，又由于观察者里的if都有清空的操作
                 * ，导致程序无限循环。
                 *
                 * 解决办法:
                 * 在被观察者那里设置布尔判断，
                 * 先把第1组的被观察者的布尔值设为true，表示可以运行
                 * 只要点击第1组group，就把第2组的被观察者的布尔值设为false，相当于不让被观察者执行
                 * 并执行第2组的状态清空
                 *
                 * 又是先把第2组的布尔值设为true,开放被观察者
                 * 把第1组布尔值设为false,关闭第1组的被观察者
                 * 当点击第2组group，就自然的只运行第2组的观察者，第1组的被观察者不被运行，
                 * 就不会导致程序无限循环执行
                 */

                Log.i(TAG, "clickable1 , clickable2 ====> " + clickable1 + " " + clickable2);
                if (Objects.equals(s, "r1") ) {

                    Log.i(TAG, "这是 r1 ");
                    clickable2 = false;
                    clickable1 = true;

                    // 点击，取map的数据,这个ui的改变因为是要用到showLength,
                    // 然而由下面对clearCheck运行的解释，他会再次运行要取消状态的那个group，
                    // 所以showLength会改变，所以要在showLength改变之前，改变ui
                    textView.setText(numMap.get(showLength));

                    // 运行clearCheck()的时候,会重新去group1里面找button，并且设为取消状态，
                    // 因为是group1绑定的是状态改变监听器，所以只要状态改变，都会再运行group1一次
                    radioGroup2.clearCheck();

                    /**
                     * 总之，要注意，凡是影响到RadioGroup的状态的操作，都会运行监听器一次.
                     */

                } else if (Objects.equals(s, "r2") ) {

                    Log.i(TAG, "这是 r2 ");
                    clickable1 = false;
                    clickable2 = true;

                    // 点击，取map的数据
                    textView.setText(numMap.get(showLength));

                    radioGroup1.clearCheck();
                }
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        };

        radioGroup1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.eightNum:
                        Log.i(TAG, "onCheckedChanged: 8 ");
                        showLength = 8;
                        break;
                    case R.id.tenNum:
                        Log.i(TAG, "onCheckedChanged: 10 ");
                        showLength = 10;
                        break;
                    case R.id.thirteenNum:
                        Log.i(TAG, "onCheckedChanged: 13 ");
                        showLength = 13;
                        break;
                    case R.id.superNum:
                        Log.i(TAG, "onCheckedChanged: 15 ");
                        showLength = 15;
                        break;
                }
                if (clickable1) {
                    Observable.just("r1").subscribe(observer);
                    clickable2 = true;
                }
            }
        });

        radioGroup2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.noChanged:
                        Log.i(TAG, "onCheckedChanged: 32 ");
                        showLength = 32;
                        break;
                    case R.id.eightSuper:
                        Log.i(TAG, "onCheckedChanged: -8 ");
                        showLength = -8;
                        break;
                    case R.id.tenSuper:
                        Log.i(TAG, "onCheckedChanged: -10 ");
                        showLength = -10;
                        break;
                    case R.id.thirteenSuper:
                        Log.i(TAG, "onCheckedChanged: -13 ");
                        showLength = -13;
                        break;
                }
                if (clickable2) {
                    Observable.just("r2").subscribe(observer);
                    clickable1 = true;
                }

            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                radioGroup1.clearCheck();
                radioGroup2.clearCheck();
                textView.setText("");
            }
        });
    }

    private String getMD5(String content) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(content.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("NoSuchAlgorithmException", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UnsupportedEncodingException", e);
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10) {
                hex.append("0");
            }
            hex.append(Integer.toHexString(b & 0xFF));
        }
        return hex.toString();
    }

    //为了避免过多使用if，使用map来优化
    private void dataPutMap(String content) {
        numMap = new ArrayMap<>();
        numMap.put(8, content.substring(18, 26));
        numMap.put(10, content.substring(8, 18));
        numMap.put(13, content.substring(16, 29));
        numMap.put(15, superNum(content.substring(8, 16)));
        numMap.put(-8, superNum(content.substring(18, 26)));
        numMap.put(-10, superNum(content.substring(8, 18)));
        numMap.put(-13, superNum(content.substring(16, 29)));
        numMap.put(32, content);
    }

    public String superNum(String content) {
        String[] symbol = new String[]{"@", "$", "^", "*", "!", "<", ">", "?", "[", "]", "#", "/"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            sb.append(c);
            //这里不能设置 i>= 1 因为，symbol的0处的数据会丢失，
            // 而且symbol的数组长度也不够长
            if (i < content.length() - 1) {
                sb.append(symbol[i]);
            }
        }
        return sb.toString();
    }

    private void computer() {
        String encoder = editText.getText().toString().trim();
        String mySalt = editText2.getText().toString().trim();

        /*
         这里要注意，不能用mySalt == null 来判断EditText是否为空
         */
        if (!Objects.equals(mySalt, "")) {
            encoder = encoder + "@" + mySalt + "$";
            myMD5 = getMD5(encoder);
        } else {
            myMD5 = getMD5(encoder);
        }

        //Log.i(TAG, "onClick: len " + myMD5.length());
    }
}
