package org.autojs.autojs.ui.widget;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;

import androidx.annotation.NonNull;
import android.util.AttributeSet;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import org.autojs.autojs.theme.dialog.ThemeColorMaterialDialogBuilder;

import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.Collections;

/**
 * Created by Stardust on 2017/3/5.
 */

public class CommonMarkdownView extends WebView {

    public interface OnPageFinishedListener {
        void onPageFinished(WebView view, String url);
    }

    private final Parser mParser = Parser.builder().build();
    private final HtmlRenderer mHtmlRender = HtmlRenderer.builder()
            .extensions(Collections.singleton(new HeadingAnchorExtension.Builder().build()))
            .build();

    private String mMarkdownHtml;
    private String mPadding = "16px";
    private OnPageFinishedListener mOnPageFinishedListener;

    public CommonMarkdownView(Context context) {
        super(context);
        init();
    }

    public CommonMarkdownView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CommonMarkdownView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setPadding(String padding) {
        mPadding = padding;
    }

    private void init() {
        getSettings().setJavaScriptEnabled(true);
        addJavascriptInterface(new CodeCopyBridge(), "AndroidCodeBridge");
        setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                loadUrl("javascript:document.body.style.margin=\"" + mPadding + "\"; void 0");
                injectCodeCopyButtons();
                if (mOnPageFinishedListener != null) {
                    mOnPageFinishedListener.onPageFinished(view, url);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                getContext().startActivity(new Intent(Intent.ACTION_VIEW).setData(request.getUrl()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                getContext().startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                return true;
            }

        });
    }

    private void injectCodeCopyButtons() {
        String script = "javascript:(function(){"
                + "try{"
                + "var blocks=document.querySelectorAll('pre');"
                + "for(var i=0;i<blocks.length;i++){"
                + "var pre=blocks[i];"
                + "if(pre.getAttribute('data-copy-ready')==='1'){continue;}"
                + "pre.setAttribute('data-copy-ready','1');"
                + "pre.style.position='relative';"
                + "var btn=document.createElement('button');"
                + "btn.innerText='复制';"
                + "btn.style.position='absolute';"
                + "btn.style.top='8px';"
                + "btn.style.right='8px';"
                + "btn.style.border='1px solid #00F4FE';"
                + "btn.style.background='#121820';"
                + "btn.style.color='#00F4FE';"
                + "btn.style.borderRadius='6px';"
                + "btn.style.padding='2px 8px';"
                + "btn.style.fontSize='12px';"
                + "btn.style.cursor='pointer';"
                + "btn.onclick=(function(targetPre,targetBtn){"
                + "return function(){"
                + "var codeNode=targetPre.querySelector('code');"
                + "var text=codeNode?codeNode.innerText:targetPre.innerText;"
                + "if(window.AndroidCodeBridge&&window.AndroidCodeBridge.copyCode){"
                + "window.AndroidCodeBridge.copyCode(text||'');"
                + "targetBtn.innerText='已复制';"
                + "setTimeout(function(){targetBtn.innerText='复制';},1200);"
                + "}"
                + "};"
                + "})(pre,btn);"
                + "pre.appendChild(btn);"
                + "}"
                + "}catch(e){}"
                + "})();";
        loadUrl(script);
    }

    private class CodeCopyBridge {
        @JavascriptInterface
        public void copyCode(String code) {
            ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clipData = ClipData.newPlainText("code", code);
                clipboard.setPrimaryClip(clipData);
                post(() -> android.widget.Toast.makeText(getContext(), "代码已复制", android.widget.Toast.LENGTH_SHORT).show());
            }
        }
    }


    public void loadMarkdown(String markdown) {
        mMarkdownHtml = renderMarkdown(markdown);
        loadHtml(mMarkdownHtml);
    }

    private void loadHtml(String html) {
        loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    public void loadMarkdown(String markdown, String baseUrl) {
        mMarkdownHtml = renderMarkdown(markdown);
        loadDataWithBaseURL(baseUrl, mMarkdownHtml, "text/html", "utf-8", null);
    }

    private String renderMarkdown(String markdown) {
        Node document = mParser.parse(markdown);
        String body = mHtmlRender.render(document);
        return "<!doctype html><html><head>"
                + "<meta charset=\"utf-8\"/>"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1,maximum-scale=1\"/>"
                + "<style>"
                + "html,body{margin:0;padding:0;background:#121820;color:#F1F3FC;line-height:1.65;font-size:15px;word-break:break-word;}"
                + "body *{color:#F1F3FC !important;}"
                + "h1,h2,h3,h4,h5,h6{color:#9CFF93;line-height:1.35;margin:18px 0 10px;}"
                + "h1 *,h2 *,h3 *,h4 *,h5 *,h6 *{color:#9CFF93 !important;}"
                + "p,li,pre,code,blockquote,table,span,div,strong,em{color:#F1F3FC !important;}"
                + "a,a *{color:#00F4FE !important;text-decoration:none;}"
                + "pre,code{background:#0F141A !important;border-radius:8px;}"
                + "pre{padding:10px;overflow:auto;}"
                + "img{max-width:100%;height:auto;border-radius:8px;}"
                + "blockquote{border-left:3px solid #00F4FE;padding-left:10px;color:#A8ABB3 !important;}"
                + "</style>"
                + "</head><body>"
                + body
                + "</body></html>";
    }

    public void setText(int resId) {
        setText(getContext().getString(resId));
    }

    private void setText(String text) {
        loadDataWithBaseURL(null, text, "text/plain", "utf-8", null);
    }

    public void goBack() {
        super.goBack();
        if (!canGoBack() && mMarkdownHtml != null) {
            loadHtml(mMarkdownHtml);
        }
    }

    public static class DialogBuilder extends ThemeColorMaterialDialogBuilder {

        private final CommonMarkdownView mMarkdownView;
        private final FrameLayout mContainer;

        public DialogBuilder(@NonNull Context context) {
            super(context);
            mContainer = new FrameLayout(context);
            mMarkdownView = new CommonMarkdownView(context);
            mContainer.addView(mMarkdownView);
            mContainer.setClipToPadding(true);
            customView(mContainer, false);
        }

        public DialogBuilder padding(int l, int t, int r, int b) {
            mContainer.setPadding(l, t, r, b);
            return this;
        }

        public DialogBuilder markdown(String md) {
            mMarkdownView.loadMarkdown(md);
            return this;
        }

    }
}