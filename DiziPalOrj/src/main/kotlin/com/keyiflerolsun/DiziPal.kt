import android.content.SharedPreferences;
import com.lagradost.api.Log;
import com.lagradost.cloudstream3.Episode;
import com.lagradost.cloudstream3.MainAPI;
import com.lagradost.cloudstream3.MainAPIKt;
import com.lagradost.cloudstream3.MainPageData;
import com.lagradost.cloudstream3.MovieSearchResponse;
import com.lagradost.cloudstream3.Score;
import com.lagradost.cloudstream3.SearchResponse;
import com.lagradost.cloudstream3.TvSeriesSearchResponse;
import com.lagradost.cloudstream3.TvType;
import com.lagradost.cloudstream3.network.CloudflareKiller;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import kotlin.Lazy;
import kotlin.LazyKt;
import kotlin.Metadata;
import kotlin.Pair;
import kotlin.TuplesKt;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.collections.SetsKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.text.StringsKt;
import kotlinx.coroutines.BuildersKt;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

@SourceDebugExtension({"SMAP\nDiziPal.kt\nKotlin\n*S Kotlin\n*F\n+ 1 DiziPal.kt\ncom/kraptor/DiziPal\n+ 2 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n+ 4 ArraysJVM.kt\nkotlin/collections/ArraysKt__ArraysJVMKt\n*L\n1#1,457:1\n1915#2,2:458\n1642#2,10:460\n1915#2:470\n1916#2:472\n1652#2:473\n1642#2,10:474\n1915#2:484\n1916#2:486\n1652#2:487\n1915#2,2:489\n1586#2:491\n1661#2,3:492\n1586#2:495\n1661#2,3:496\n1586#2:499\n1661#2,3:500\n1586#2:503\n1661#2,3:504\n1586#2:507\n1661#2,3:508\n1915#2,2:513\n1807#2,3:515\n1606#2:518\n1617#2:519\n1924#2,2:520\n1926#2:523\n1618#2:524\n1#3:471\n1#3:485\n1#3:488\n1#3:522\n37#4,2:511\n*S KotlinDebug\n*F\n+ 1 DiziPal.kt\ncom/kraptor/DiziPal\n*L\n131#1:458,2\n164#1:460,10\n164#1:470\n164#1:472\n164#1:473\n166#1:474,10\n166#1:484\n166#1:486\n166#1:487\n229#1:489,2\n287#1:491\n287#1:492,3\n288#1:495\n288#1:496,3\n292#1:499\n292#1:500,3\n299#1:503\n299#1:504,3\n329#1:507\n329#1:508,3\n399#1:513,2\n427#1:515,3\n313#1:518\n313#1:519\n313#1:520,2\n313#1:523\n313#1:524\n164#1:471\n166#1:485\n313#1:522\n343#1:511,2\n*E\n"})
@Metadata(d1 = {"\u0000 \u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\b\n\u0002\u0010\u000b\n\u0002\b\b\n\u0002\u0010\"\n\u0002\u0018\u0002\n\u0002\b\u0007\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010%\n\u0002\u0018\u0002\n\u0002\u0010\t\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\u0018\u0000 U2\u00020\u0001:\u0002TUB\u0013\u0012\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\u001e\u0010/\u001a\u0002092\u0006\u0010:\u001a\u0002082\u0006\u0010;\u001a\u00020<H@¢\u0006\u0002\u0010=J\u000e\u0010>\u001a\u0004\u0018\u000105*\u00020?H\u0002J\u000e\u0010@\u001a\u0004\u0018\u000105*\u00020?H\u0002J\u001e\u0010A\u001a\u00020B2\u0006\u0010C\u001a\u00020\u00072\u0006\u0010:\u001a\u000208H@¢\u0006\u0002\u0010DJ\u001e\u0010E\u001a\n\u0012\u0004\u0012\u000205\u0018\u00010-2\u0006\u0010C\u001a\u00020\u0007H@¢\u0006\u0002\u0010FJ\u0018\u0010G\u001a\u0004\u0018\u00010H2\u0006\u0010I\u001a\u00020\u0007H@¢\u0006\u0002\u0010FJF\u0010J\u001a\u00020\u00102\u0006\u0010K\u001a\u00020\u00072\u0006\u0010L\u001a\u00020\u00102\u0012\u0010M\u001a\u000e\u0012\u0004\u0012\u00020O\u0012\u0004\u0012\u00020P0N2\u0012\u0010Q\u001a\u000e\u0012\u0004\u0012\u00020R\u0012\u0004\u0012\u00020P0NH@¢\u0006\u0002\u0010SR\u001a\u0010\u0006\u001a\u00020\u0007X\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\b\u0010\t\"\u0004\b\n\u0010\u000bR\u001a\u0010\f\u001a\u00020\u0007X\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\r\u0010\t\"\u0004\b\u000e\u0010\u000bR\u0014\u0010\u000f\u001a\u00020\u0010XD¢\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u001a\u0010\u0013\u001a\u00020\u0007X\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u0014\u0010\t\"\u0004\b\u0015\u0010\u000bR\u0014\u0010\u0016\u001a\u00020\u0010XD¢\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0012R\u001a\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u001a0\u0019X\u0004¢\u0006\b\n\u0000\u001a\u0004\b\u001b\u0010\u001cR\u001a\u0010\u001d\u001a\u00020\u0010X\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u001e\u0010\u0012\"\u0004\b\u001f\u0010 R\u001b\u0010!\u001a\u00020\"8BX\u0002¢\u0006\f\n\u0004\b%\u0010&\u001a\u0004\b#\u0010$R\u001b\u0010'\u001a\u00020(8BX\u0002¢\u0006\f\n\u0004\b+\u0010&\u001a\u0004\b)\u0010*R\u001a\u0010,\u001a\b\u0012\u0004\u0012\u00020.0-X\u0004¢\u0006\b\n\u0000\u001a\u0004\b/\u00100R,\u00101\u001a \u0012\u0004\u0012\u00020\u0007\u0012\u0016\u0012\u0014\u0012\u0004\u0012\u000204\u0012\n\u0012\b\u0012\u0004\u0012\u0002050-0302X\u0004¢\u0006\u0002\n\u0000R\u000e\u00106\u001a\u00020\u0010X\u000e¢\u0006\u0002\n\u0000R\u000e\u00107\u001a\u000208X\u0004¢\u0006\u0002\n\u0000¨\u0006V"}, d2 = {"Lcom/kraptor/DiziPal;", "Lcom/lagradost/cloudstream3/MainAPI;", "sharedPref", "Landroid/content/SharedPreferences;", "<init>", "(Landroid/content/SharedPreferences;)V", "mainUrl", "", "getMainUrl", "()Ljava/lang/String;", "setMainUrl", "(Ljava/lang/String;)V", "name", "getName", "setName", "hasMainPage", "", "getHasMainPage", "()Z", "lang", "getLang", "setLang", "hasQuickSearch", "getHasQuickSearch", "supportedTypes", "", "Lcom/lagradost/cloudstream3/TvType;", "getSupportedTypes", "()Ljava/util/Set;", "sequentialMainPage", "getSequentialMainPage", "setSequentialMainPage", "(Z)V", "cloudflareKiller", "Lcom/lagradost/cloudstream3/network/CloudflareKiller;", "getCloudflareKiller", "()Lcom/lagradost/cloudstream3/network/CloudflareKiller;", "cloudflareKiller$delegate", "Lkotlin/Lazy;", "interceptor", "Lcom/kraptor/DiziPal$CloudflareInterceptor;", "getInterceptor", "()Lcom/kraptor/DiziPal$CloudflareInterceptor;", "interceptor$delegate", "mainPage", "", "Lcom/lagradost/cloudstream3/MainPageData;", "getMainPage", "()Ljava/util/List;", "homeCache", "", "Lkotlin/Pair;", "", "Lcom/lagradost/cloudstream3/SearchResponse;", "isPreloading", "CACHE_TIMEOUT", "", "Lcom/lagradost/cloudstream3/HomePageResponse;", "page", "request", "Lcom/lagradost/cloudstream3/MainPageRequest;", "(ILcom/lagradost/cloudstream3/MainPageRequest;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "toMainSearchResult", "Lorg/jsoup/nodes/Element;", "sonBolumler", "search", "Lcom/lagradost/cloudstream3/SearchResponseList;", "query", "(Ljava/lang/String;ILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "quickSearch", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "load", "Lcom/lagradost/cloudstream3/LoadResponse;", "url", "loadLinks", "data", "isCasting", "subtitleCallback", "Lkotlin/Function1;", "Lcom/lagradost/cloudstream3/SubtitleFile;", "", "callback", "Lcom/lagradost/cloudstream3/utils/ExtractorLink;", "(Ljava/lang/String;ZLkotlin/jvm/functions/Function1;Lkotlin/jvm/functions/Function1;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "CloudflareInterceptor", "Companion", "DiziPal_debug"}, k = 1, mv = {2, 3, 0}, xi = 48)
/* compiled from: DiziPal.kt */
public final class DiziPal extends MainAPI {
    private static final long CACHE_DURATION = 300000;
    @NotNull
    public static final Companion Companion = new Companion((DefaultConstructorMarker) null);
    @NotNull
    private static final String LAST_UPDATE_KEY = "last_domain_update";
    private final int CACHE_TIMEOUT;
    @NotNull
    private final Lazy cloudflareKiller$delegate;
    private final boolean hasMainPage;
    private final boolean hasQuickSearch;
    /* access modifiers changed from: private */
    @NotNull
    public final Map<String, Pair<Long, List<SearchResponse>>> homeCache;
    @NotNull
    private final Lazy interceptor$delegate;
    private boolean isPreloading;
    @NotNull
    private String lang;
    @NotNull
    private final List<MainPageData> mainPage;
    @NotNull
    private String mainUrl;
    @NotNull
    private String name;
    private boolean sequentialMainPage;
    @NotNull
    private final Set<TvType> supportedTypes;

    public DiziPal() {
        this((SharedPreferences) null, 1, (DefaultConstructorMarker) null);
    }

    public DiziPal(@Nullable SharedPreferences sharedPref) {
        this.mainUrl = Companion.getDomain(sharedPref, "DiziPal");
        this.name = "DiziPal";
        this.hasMainPage = true;
        this.lang = "tr";
        this.hasQuickSearch = true;
        this.supportedTypes = SetsKt.setOf(new TvType[]{TvType.TvSeries, TvType.Movie});
        this.sequentialMainPage = true;
        this.cloudflareKiller$delegate = LazyKt.lazy(new DiziPal$$ExternalSyntheticLambda3());
        this.interceptor$delegate = LazyKt.lazy(new DiziPal$$ExternalSyntheticLambda4(this));
        this.mainPage = MainAPIKt.mainPageOf(new Pair[]{TuplesKt.to(getMainUrl() + "/filmler", "Yeni Filmler"), TuplesKt.to(getMainUrl() + "/diziler/", "Yeni Diziler"), TuplesKt.to(getMainUrl() + "/animeler/", "Animeler"), TuplesKt.to(getMainUrl() + "/platform/netflix", "Netflix"), TuplesKt.to(getMainUrl() + "/platform/exxen", "Exxen"), TuplesKt.to(getMainUrl() + "/platform/prime-video", "Prime Video"), TuplesKt.to(getMainUrl() + "/platform/tabii", "Tabii"), TuplesKt.to(getMainUrl() + "/platform/disney", "Disney+"), TuplesKt.to(getMainUrl() + "/platform/hbomax", "HBOMax"), TuplesKt.to(getMainUrl() + "/dizi-kategori/aksiyon", "Aksiyon Dizileri"), TuplesKt.to(getMainUrl() + "/dizi-kategori/bilim-kurgu", "Bilim Kurgu Dizileri"), TuplesKt.to(getMainUrl() + "/dizi-kategori/dram", "Dram Dizileri"), TuplesKt.to(getMainUrl() + "/dizi-kategori/komedi", "Komedi Dizileri"), TuplesKt.to(getMainUrl() + "/dizi-kategori/romantik", "Romantik Dizileri"), TuplesKt.to(getMainUrl() + "/dizi-kategori/savas", "Savaş Dizileri")});
        this.homeCache = new LinkedHashMap();
        this.CACHE_TIMEOUT = 7200000;
    }

    /* JADX INFO: this call moved to the top of the method (can break code semantics) */
    public /* synthetic */ DiziPal(SharedPreferences sharedPreferences, int i, DefaultConstructorMarker defaultConstructorMarker) {
        this((i & 1) != 0 ? null : sharedPreferences);
    }

    @NotNull
    public String getMainUrl() {
        return this.mainUrl;
    }

    public void setMainUrl(@NotNull String str) {
        this.mainUrl = str;
    }

    @NotNull
    public String getName() {
        return this.name;
    }

    public void setName(@NotNull String str) {
        this.name = str;
    }

    public boolean getHasMainPage() {
        return this.hasMainPage;
    }

    @NotNull
    public String getLang() {
        return this.lang;
    }

    public void setLang(@NotNull String str) {
        this.lang = str;
    }

    public boolean getHasQuickSearch() {
        return this.hasQuickSearch;
    }

    @NotNull
    public Set<TvType> getSupportedTypes() {
        return this.supportedTypes;
    }

    public boolean getSequentialMainPage() {
        return this.sequentialMainPage;
    }

    public void setSequentialMainPage(boolean z) {
        this.sequentialMainPage = z;
    }

    /* access modifiers changed from: private */
    public static final CloudflareKiller cloudflareKiller_delegate$lambda$0() {
        return new CloudflareKiller();
    }

    private final CloudflareKiller getCloudflareKiller() {
        return (CloudflareKiller) this.cloudflareKiller$delegate.getValue();
    }

    /* access modifiers changed from: private */
    public final CloudflareInterceptor getInterceptor() {
        return (CloudflareInterceptor) this.interceptor$delegate.getValue();
    }

    /* access modifiers changed from: private */
    public static final CloudflareInterceptor interceptor_delegate$lambda$0(DiziPal this$0) {
        return new CloudflareInterceptor(this$0.getCloudflareKiller());
    }

    @Metadata(d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B\u000f\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0004\b\u0004\u0010\u0005J\u0010\u0010\u0006\u001a\u00020\u00072\u0006\u0010\b\u001a\u00020\tH\u0016R\u000e\u0010\u0002\u001a\u00020\u0003X\u0004¢\u0006\u0002\n\u0000¨\u0006\n"}, d2 = {"Lcom/kraptor/DiziPal$CloudflareInterceptor;", "Lokhttp3/Interceptor;", "cloudflareKiller", "Lcom/lagradost/cloudstream3/network/CloudflareKiller;", "<init>", "(Lcom/lagradost/cloudstream3/network/CloudflareKiller;)V", "intercept", "Lokhttp3/Response;", "chain", "Lokhttp3/Interceptor$Chain;", "DiziPal_debug"}, k = 1, mv = {2, 3, 0}, xi = 48)
    /* compiled from: DiziPal.kt */
    public static final class CloudflareInterceptor implements Interceptor {
        @NotNull
        private final CloudflareKiller cloudflareKiller;

        public CloudflareInterceptor(@NotNull CloudflareKiller cloudflareKiller2) {
            this.cloudflareKiller = cloudflareKiller2;
        }

        @NotNull
        public Response intercept(@NotNull Interceptor.Chain chain) {
            Response response = chain.proceed(chain.request());
            if (!StringsKt.contains$default(Jsoup.parse(response.peekBody(1048576).string()).html(), "Just a moment", false, 2, (Object) null)) {
                return response;
            }
            Log.INSTANCE.d("kraptor_Dizipal", "!!sikik cloudflare geldi!!");
            return this.cloudflareKiller.intercept(chain);
        }
    }

    @Metadata(d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u0003\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J\u001a\u0010\b\u001a\u00020\u00072\b\u0010\t\u001a\u0004\u0018\u00010\n2\u0006\u0010\u000b\u001a\u00020\u0007H\u0002R\u000e\u0010\u0004\u001a\u00020\u0005XT¢\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007XT¢\u0006\u0002\n\u0000¨\u0006\f"}, d2 = {"Lcom/kraptor/DiziPal$Companion;", "", "<init>", "()V", "CACHE_DURATION", "", "LAST_UPDATE_KEY", "", "getDomain", "sharedPref", "Landroid/content/SharedPreferences;", "providerName", "DiziPal_debug"}, k = 1, mv = {2, 3, 0}, xi = 48)
    /* compiled from: DiziPal.kt */
    public static final class Companion {
        public /* synthetic */ Companion(DefaultConstructorMarker defaultConstructorMarker) {
            this();
        }

        private Companion() {
        }

        /* access modifiers changed from: private */
        public final String getDomain(SharedPreferences sharedPref, String providerName) {
            return (String) BuildersKt.runBlocking$default((CoroutineContext) null, new DiziPal$Companion$getDomain$1(sharedPref, providerName, (Continuation<? super DiziPal$Companion$getDomain$1>) null), 1, (Object) null);
        }
    }

    @NotNull
    public List<MainPageData> getMainPage() {
        return this.mainPage;
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r8v8, resolved type: java.util.Collection} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r1v19, resolved type: java.util.List} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r8v10, resolved type: java.util.Collection} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r2v5, resolved type: java.util.List} */
    /* JADX WARNING: Multi-variable type inference failed */
    /* JADX WARNING: Removed duplicated region for block: B:10:0x0033  */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x0054  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x01db  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x0225  */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x02b0  */
    /* JADX WARNING: Removed duplicated region for block: B:72:0x02b2  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x02b6  */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x002b  */
    @org.jetbrains.annotations.Nullable
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.lang.Object getMainPage(int r30, @org.jetbrains.annotations.NotNull com.lagradost.cloudstream3.MainPageRequest r31, @org.jetbrains.annotations.NotNull kotlin.coroutines.Continuation<? super com.lagradost.cloudstream3.HomePageResponse> r32) {
        /*
            r29 = this;
            r0 = r29
            r1 = r30
            r2 = r32
            boolean r3 = r2 instanceof com.kraptor.DiziPal$getMainPage$1
            if (r3 == 0) goto L_0x001a
            r3 = r2
            com.kraptor.DiziPal$getMainPage$1 r3 = (com.kraptor.DiziPal$getMainPage$1) r3
            int r4 = r3.label
            r5 = -2147483648(0xffffffff80000000, float:-0.0)
            r4 = r4 & r5
            if (r4 == 0) goto L_0x001a
            int r4 = r3.label
            int r4 = r4 - r5
            r3.label = r4
            goto L_0x001f
        L_0x001a:
            com.kraptor.DiziPal$getMainPage$1 r3 = new com.kraptor.DiziPal$getMainPage$1
            r3.<init>(r0, r2)
        L_0x001f:
            java.lang.Object r4 = r3.result
            java.lang.Object r5 = kotlin.coroutines.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED()
            int r6 = r3.label
            r10 = 1
            switch(r6) {
                case 0: goto L_0x0054;
                case 1: goto L_0x0033;
                default: goto L_0x002b;
            }
        L_0x002b:
            java.lang.IllegalStateException r1 = new java.lang.IllegalStateException
            java.lang.String r2 = "call to 'resume' before 'invoke' with coroutine"
            r1.<init>(r2)
            throw r1
        L_0x0033:
            long r5 = r3.J$0
            int r1 = r3.I$0
            java.lang.Object r11 = r3.L$3
            java.lang.String r11 = (java.lang.String) r11
            java.lang.Object r12 = r3.L$2
            java.lang.String r12 = (java.lang.String) r12
            java.lang.Object r13 = r3.L$1
            java.lang.String r13 = (java.lang.String) r13
            java.lang.Object r14 = r3.L$0
            com.lagradost.cloudstream3.MainPageRequest r14 = (com.lagradost.cloudstream3.MainPageRequest) r14
            kotlin.ResultKt.throwOnFailure(r4)
            r18 = r3
            r21 = r4
            r0 = 0
            r2 = 0
            r3 = r1
            r1 = 2
            goto L_0x01c8
        L_0x0054:
            kotlin.ResultKt.throwOnFailure(r4)
            long r11 = java.lang.System.currentTimeMillis()
            java.lang.String r6 = r31.getName()
            java.lang.String r13 = r31.getData()
            if (r1 > r10) goto L_0x00b1
            java.util.Map<java.lang.String, kotlin.Pair<java.lang.Long, java.util.List<com.lagradost.cloudstream3.SearchResponse>>> r14 = r0.homeCache
            java.lang.Object r14 = r14.get(r6)
            kotlin.Pair r14 = (kotlin.Pair) r14
            if (r14 == 0) goto L_0x00b1
            java.lang.Object r15 = r14.getFirst()
            java.lang.Number r15 = (java.lang.Number) r15
            long r15 = r15.longValue()
            long r15 = r11 - r15
            int r7 = r0.CACHE_TIMEOUT
            long r8 = (long) r7
            int r7 = (r15 > r8 ? 1 : (r15 == r8 ? 0 : -1))
            if (r7 >= 0) goto L_0x00b1
            com.lagradost.api.Log r5 = com.lagradost.api.Log.INSTANCE
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r8 = "⚡ CACHE HIT: "
            java.lang.StringBuilder r7 = r7.append(r8)
            java.lang.StringBuilder r7 = r7.append(r6)
            java.lang.String r8 = " | Anında yüklendi"
            java.lang.StringBuilder r7 = r7.append(r8)
            java.lang.String r7 = r7.toString()
            java.lang.String r8 = "CS_PERF"
            r5.d(r8, r7)
            java.lang.Object r5 = r14.getSecond()
            java.util.List r5 = (java.util.List) r5
            java.lang.Boolean r7 = kotlin.coroutines.jvm.internal.Boxing.boxBoolean(r10)
            com.lagradost.cloudstream3.HomePageResponse r5 = com.lagradost.cloudstream3.MainAPIKt.newHomePageResponse(r6, r5, r7)
            return r5
        L_0x00b1:
            if (r1 > r10) goto L_0x0121
            boolean r7 = r0.isPreloading
            if (r7 != 0) goto L_0x0121
            java.util.Map<java.lang.String, kotlin.Pair<java.lang.Long, java.util.List<com.lagradost.cloudstream3.SearchResponse>>> r7 = r0.homeCache
            boolean r7 = r7.isEmpty()
            if (r7 == 0) goto L_0x0121
            r0.isPreloading = r10
            java.util.List r7 = r0.getMainPage()
            java.lang.Iterable r7 = (java.lang.Iterable) r7
            r8 = 0
            java.util.Iterator r9 = r7.iterator()
        L_0x00cc:
            boolean r14 = r9.hasNext()
            if (r14 == 0) goto L_0x011c
            java.lang.Object r14 = r9.next()
            r15 = r14
            com.lagradost.cloudstream3.MainPageData r15 = (com.lagradost.cloudstream3.MainPageData) r15
            r16 = 0
            java.lang.String r10 = r15.getData()
            java.lang.String r2 = r15.getName()
            boolean r21 = kotlin.jvm.internal.Intrinsics.areEqual(r2, r6)
            if (r21 != 0) goto L_0x010f
            kotlinx.coroutines.GlobalScope r21 = kotlinx.coroutines.GlobalScope.INSTANCE
            r22 = r21
            kotlinx.coroutines.CoroutineScope r22 = (kotlinx.coroutines.CoroutineScope) r22
            kotlinx.coroutines.CoroutineDispatcher r21 = kotlinx.coroutines.Dispatchers.getIO()
            r23 = r21
            kotlin.coroutines.CoroutineContext r23 = (kotlin.coroutines.CoroutineContext) r23
            r21 = r4
            com.kraptor.DiziPal$getMainPage$2$1 r4 = new com.kraptor.DiziPal$getMainPage$2$1
            r28 = r5
            r5 = 0
            r4.<init>(r10, r0, r2, r5)
            r25 = r4
            kotlin.jvm.functions.Function2 r25 = (kotlin.jvm.functions.Function2) r25
            r26 = 2
            r27 = 0
            r24 = 0
            kotlinx.coroutines.BuildersKt.launch$default(r22, r23, r24, r25, r26, r27)
            goto L_0x0113
        L_0x010f:
            r21 = r4
            r28 = r5
        L_0x0113:
            r2 = r32
            r4 = r21
            r5 = r28
            r10 = 1
            goto L_0x00cc
        L_0x011c:
            r21 = r4
            r28 = r5
            goto L_0x0125
        L_0x0121:
            r21 = r4
            r28 = r5
        L_0x0125:
            r2 = 1
            if (r1 > r2) goto L_0x012d
            r2 = r13
            r4 = 2
            r5 = 0
            r7 = 0
            goto L_0x0161
        L_0x012d:
            java.lang.String r2 = "/"
            r4 = 2
            r5 = 0
            r7 = 0
            boolean r2 = kotlin.text.StringsKt.endsWith$default(r13, r2, r7, r4, r5)
            r8 = 47
            if (r2 == 0) goto L_0x0146
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.StringBuilder r2 = r2.append(r13)
            java.lang.String r9 = "page/"
            goto L_0x0151
        L_0x0146:
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            java.lang.StringBuilder r2 = r2.append(r13)
            java.lang.String r9 = "/page/"
        L_0x0151:
            java.lang.StringBuilder r2 = r2.append(r9)
            java.lang.StringBuilder r2 = r2.append(r1)
            java.lang.StringBuilder r2 = r2.append(r8)
            java.lang.String r2 = r2.toString()
        L_0x0161:
            r17 = 2
            com.lagradost.nicehttp.Requests r4 = com.lagradost.cloudstream3.MainActivityKt.getApp()
            com.kraptor.DiziPal$CloudflareInterceptor r8 = r0.getInterceptor()
            r15 = r8
            okhttp3.Interceptor r15 = (okhttp3.Interceptor) r15
            java.lang.Object r8 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r31)
            r3.L$0 = r8
            r3.L$1 = r6
            r3.L$2 = r13
            java.lang.Object r8 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r2)
            r3.L$3 = r8
            r3.I$0 = r1
            r3.J$0 = r11
            r8 = 1
            r3.label = r8
            r9 = r6
            r6 = 0
            r19 = 0
            r7 = 0
            r20 = 1
            r8 = 0
            r10 = r9
            r9 = 0
            r14 = r10
            r10 = 0
            r22 = r11
            r11 = 0
            r12 = 0
            r18 = r13
            r16 = r14
            r13 = 0
            r24 = r16
            r16 = 0
            r25 = 2
            r17 = 0
            r26 = 0
            r19 = 3582(0xdfe, float:5.02E-42)
            r27 = 1
            r20 = 0
            r0 = r5
            r25 = r18
            r1 = 2
            r5 = r2
            r18 = r3
            r3 = r28
            r2 = 0
            java.lang.Object r4 = com.lagradost.nicehttp.Requests.get$default(r4, r5, r6, r7, r8, r9, r10, r11, r12, r13, r15, r16, r17, r18, r19, r20)
            if (r4 != r3) goto L_0x01bd
            return r3
        L_0x01bd:
            r3 = r30
            r14 = r31
            r11 = r5
            r5 = r22
            r13 = r24
            r12 = r25
        L_0x01c8:
            com.lagradost.nicehttp.NiceResponse r4 = (com.lagradost.nicehttp.NiceResponse) r4
            org.jsoup.nodes.Document r7 = r4.getDocument()
            r8 = r12
            java.lang.CharSequence r8 = (java.lang.CharSequence) r8
            java.lang.String r9 = "/diziler/son-bolumler"
            java.lang.CharSequence r9 = (java.lang.CharSequence) r9
            boolean r0 = kotlin.text.StringsKt.contains$default(r8, r9, r2, r1, r0)
            if (r0 == 0) goto L_0x0225
            java.lang.String r0 = "div.episode-item"
            org.jsoup.select.Elements r0 = r7.select(r0)
            java.lang.Iterable r0 = (java.lang.Iterable) r0
            r1 = 0
            java.util.ArrayList r8 = new java.util.ArrayList
            r8.<init>()
            java.util.Collection r8 = (java.util.Collection) r8
            r9 = r0
            r10 = 0
            r15 = r9
            r16 = 0
            java.util.Iterator r17 = r15.iterator()
        L_0x01f4:
            boolean r19 = r17.hasNext()
            if (r19 == 0) goto L_0x021b
            java.lang.Object r19 = r17.next()
            r20 = r19
            r22 = 0
            r2 = r20
            org.jsoup.nodes.Element r2 = (org.jsoup.nodes.Element) r2
            r23 = 0
            r30 = r0
            r0 = r29
            com.lagradost.cloudstream3.SearchResponse r2 = r0.sonBolumler(r2)
            if (r2 == 0) goto L_0x0217
            r23 = 0
            r8.add(r2)
        L_0x0217:
            r0 = r30
            r2 = 0
            goto L_0x01f4
        L_0x021b:
            r30 = r0
            r0 = r29
            r2 = r8
            java.util.List r2 = (java.util.List) r2
            goto L_0x026b
        L_0x0225:
            r0 = r29
            java.lang.String r1 = "div.post-item"
            org.jsoup.select.Elements r1 = r7.select(r1)
            java.lang.Iterable r1 = (java.lang.Iterable) r1
            r2 = 0
            java.util.ArrayList r8 = new java.util.ArrayList
            r8.<init>()
            java.util.Collection r8 = (java.util.Collection) r8
            r9 = r1
            r10 = 0
            r15 = r9
            r16 = 0
            java.util.Iterator r17 = r15.iterator()
        L_0x0240:
            boolean r19 = r17.hasNext()
            if (r19 == 0) goto L_0x0264
            java.lang.Object r19 = r17.next()
            r20 = r19
            r22 = 0
            r30 = r1
            r1 = r20
            org.jsoup.nodes.Element r1 = (org.jsoup.nodes.Element) r1
            r23 = 0
            com.lagradost.cloudstream3.SearchResponse r1 = r0.toMainSearchResult(r1)
            if (r1 == 0) goto L_0x0261
            r23 = 0
            r8.add(r1)
        L_0x0261:
            r1 = r30
            goto L_0x0240
        L_0x0264:
            r30 = r1
            r1 = r8
            java.util.List r1 = (java.util.List) r1
            r2 = r1
        L_0x026b:
            r8 = 1
            if (r3 > r8) goto L_0x028a
            r1 = r2
            java.util.Collection r1 = (java.util.Collection) r1
            boolean r1 = r1.isEmpty()
            if (r1 != 0) goto L_0x028a
            java.util.Map<java.lang.String, kotlin.Pair<java.lang.Long, java.util.List<com.lagradost.cloudstream3.SearchResponse>>> r1 = r0.homeCache
            kotlin.Pair r9 = new kotlin.Pair
            long r15 = java.lang.System.currentTimeMillis()
            java.lang.Long r10 = kotlin.coroutines.jvm.internal.Boxing.boxLong(r15)
            r9.<init>(r10, r2)
            r1.put(r13, r9)
        L_0x028a:
            long r9 = java.lang.System.currentTimeMillis()
            long r9 = r9 - r5
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r15 = "a.next, a:contains(Sonraki), a.page-numbers:contains("
            java.lang.StringBuilder r1 = r1.append(r15)
            int r15 = r3 + 1
            java.lang.StringBuilder r1 = r1.append(r15)
            r15 = 41
            java.lang.StringBuilder r1 = r1.append(r15)
            java.lang.String r1 = r1.toString()
            org.jsoup.nodes.Element r1 = r7.selectFirst(r1)
            if (r1 == 0) goto L_0x02b2
            r1 = 1
            goto L_0x02b3
        L_0x02b2:
            r1 = 0
        L_0x02b3:
            if (r1 == 0) goto L_0x02b6
            goto L_0x02b7
        L_0x02b6:
            r8 = 0
        L_0x02b7:
            java.lang.Boolean r8 = kotlin.coroutines.jvm.internal.Boxing.boxBoolean(r8)
            com.lagradost.cloudstream3.HomePageResponse r8 = com.lagradost.cloudstream3.MainAPIKt.newHomePageResponse(r13, r2, r8)
            return r8
        */
        throw new UnsupportedOperationException("Method not decompiled: com.kraptor.DiziPal.getMainPage(int, com.lagradost.cloudstream3.MainPageRequest, kotlin.coroutines.Continuation):java.lang.Object");
    }

    /* access modifiers changed from: private */
    public final SearchResponse toMainSearchResult(Element $this$toMainSearchResult) {
        String attr;
        String attr2;
        Element anchor = $this$toMainSearchResult.selectFirst("a");
        String str = null;
        if (anchor == null) {
            return null;
        }
        CharSequence attr3 = anchor.attr("title");
        if (attr3.length() == 0) {
            Element selectFirst = anchor.selectFirst("img");
            attr3 = selectFirst != null ? selectFirst.attr("alt") : null;
        }
        String str2 = (String) attr3;
        if (str2 == null) {
            return null;
        }
        String title = str2;
        String href = MainAPIKt.fixUrlNull((MainAPI) this, anchor.attr("href"));
        if (href == null) {
            return null;
        }
        MainAPI mainAPI = (MainAPI) this;
        Element selectFirst2 = $this$toMainSearchResult.selectFirst("img");
        if (selectFirst2 == null || (attr2 = selectFirst2.attr("data-srcset")) == null || (attr = StringsKt.substringBefore$default(attr2, " ", (String) null, 2, (Object) null)) == null) {
            Element selectFirst3 = $this$toMainSearchResult.selectFirst("img");
            attr = selectFirst3 != null ? selectFirst3.attr("data-src") : null;
            if (attr == null) {
                Element selectFirst4 = $this$toMainSearchResult.selectFirst("img");
                if (selectFirst4 != null) {
                    str = selectFirst4.attr("src");
                }
                String posterUrl = MainAPIKt.fixUrlNull(mainAPI, str);
                android.util.Log.d("Dizipal", "Log: Mapping Item - Title: " + title + ", Href: " + href);
                return MainAPIKt.newTvSeriesSearchResponse$default((MainAPI) this, title, href, TvType.TvSeries, false, new DiziPal$$ExternalSyntheticLambda6(posterUrl), 8, (Object) null);
            }
        }
        str = attr;
        String posterUrl2 = MainAPIKt.fixUrlNull(mainAPI, str);
        android.util.Log.d("Dizipal", "Log: Mapping Item - Title: " + title + ", Href: " + href);
        return MainAPIKt.newTvSeriesSearchResponse$default((MainAPI) this, title, href, TvType.TvSeries, false, new DiziPal$$ExternalSyntheticLambda6(posterUrl2), 8, (Object) null);
    }

    /* access modifiers changed from: private */
    public static final Unit toMainSearchResult$lambda$1(String $posterUrl, TvSeriesSearchResponse $this$newTvSeriesSearchResponse) {
        $this$newTvSeriesSearchResponse.setPosterUrl($posterUrl);
        return Unit.INSTANCE;
    }

    /* access modifiers changed from: private */
    public final SearchResponse sonBolumler(Element $this$sonBolumler) {
        String name2;
        Element selectFirst;
        String text;
        String obj;
        String replace$default;
        String episode;
        String text2;
        Element element = $this$sonBolumler;
        Element selectFirst2 = element.selectFirst("div.name");
        if (selectFirst2 == null || (name2 = selectFirst2.text()) == null || (selectFirst = element.selectFirst("div.episode")) == null || (text = selectFirst.text()) == null || (obj = StringsKt.trim(text).toString()) == null || (replace$default = StringsKt.replace$default(obj, ". Sezon ", "x", false, 4, (Object) null)) == null || (episode = StringsKt.replace$default(replace$default, ". Bölüm", "", false, 4, (Object) null)) == null) {
            return null;
        }
        String title = name2 + ' ' + episode;
        MainAPI mainAPI = (MainAPI) this;
        Element selectFirst3 = element.selectFirst("a");
        String href = MainAPIKt.fixUrlNull(mainAPI, selectFirst3 != null ? selectFirst3.attr("href") : null);
        if (href == null) {
            return null;
        }
        MainAPI mainAPI2 = (MainAPI) this;
        Element selectFirst4 = element.selectFirst("img");
        String posterUrl = MainAPIKt.fixUrlNull(mainAPI2, selectFirst4 != null ? selectFirst4.attr("src") : null);
        Element selectFirst5 = element.selectFirst("span.imdb.lessDot");
        String puan = (selectFirst5 == null || (text2 = selectFirst5.text()) == null) ? null : StringsKt.trim(text2).toString();
        android.util.Log.d("Dizipal", "Log: SonBolumler - Title: " + title);
        return MainAPIKt.newTvSeriesSearchResponse$default((MainAPI) this, title, StringsKt.substringBefore$default(href, "/sezon", (String) null, 2, (Object) null), TvType.TvSeries, false, new DiziPal$$ExternalSyntheticLambda2(posterUrl, puan), 8, (Object) null);
    }

    /* access modifiers changed from: private */
    public static final Unit sonBolumler$lambda$0(String $posterUrl, String $puan, TvSeriesSearchResponse $this$newTvSeriesSearchResponse) {
        $this$newTvSeriesSearchResponse.setPosterUrl($posterUrl);
        $this$newTvSeriesSearchResponse.setScore(Score.Companion.from10($puan));
        return Unit.INSTANCE;
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r7v60, resolved type: java.lang.Object} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r1v6, resolved type: java.lang.String} */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x016f, code lost:
        if (r7 == null) goto L_0x0174;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:47:0x019c, code lost:
        if (r9 == null) goto L_0x01a1;
     */
    /* JADX WARNING: Multi-variable type inference failed */
    /* JADX WARNING: Removed duplicated region for block: B:10:0x003b  */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x0057  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x0128  */
    /* JADX WARNING: Removed duplicated region for block: B:74:0x02a6  */
    /* JADX WARNING: Removed duplicated region for block: B:75:0x02a8  */
    /* JADX WARNING: Removed duplicated region for block: B:78:0x02b9  */
    /* JADX WARNING: Removed duplicated region for block: B:79:0x02bb  */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x0033  */
    @org.jetbrains.annotations.Nullable
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.lang.Object search(@org.jetbrains.annotations.NotNull java.lang.String r34, int r35, @org.jetbrains.annotations.NotNull kotlin.coroutines.Continuation<? super com.lagradost.cloudstream3.SearchResponseList> r36) {
        /*
            r33 = this;
            r0 = r33
            r1 = r34
            r2 = r35
            r3 = r36
            boolean r4 = r3 instanceof com.kraptor.DiziPal$search$1
            if (r4 == 0) goto L_0x001c
            r4 = r3
            com.kraptor.DiziPal$search$1 r4 = (com.kraptor.DiziPal$search$1) r4
            int r5 = r4.label
            r6 = -2147483648(0xffffffff80000000, float:-0.0)
            r5 = r5 & r6
            if (r5 == 0) goto L_0x001c
            int r5 = r4.label
            int r5 = r5 - r6
            r4.label = r5
            goto L_0x0021
        L_0x001c:
            com.kraptor.DiziPal$search$1 r4 = new com.kraptor.DiziPal$search$1
            r4.<init>(r0, r3)
        L_0x0021:
            java.lang.Object r5 = r4.result
            java.lang.Object r6 = kotlin.coroutines.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED()
            int r7 = r4.label
            java.lang.String r8 = "Dizipal"
            java.lang.String r9 = "/page/"
            java.lang.String r10 = "/?s="
            r11 = 1
            switch(r7) {
                case 0: goto L_0x0057;
                case 1: goto L_0x003b;
                default: goto L_0x0033;
            }
        L_0x0033:
            java.lang.IllegalStateException r0 = new java.lang.IllegalStateException
            java.lang.String r1 = "call to 'resume' before 'invoke' with coroutine"
            r0.<init>(r1)
            throw r0
        L_0x003b:
            int r2 = r4.I$0
            java.lang.Object r6 = r4.L$1
            java.lang.String r6 = (java.lang.String) r6
            java.lang.Object r7 = r4.L$0
            r1 = r7
            java.lang.String r1 = (java.lang.String) r1
            kotlin.ResultKt.throwOnFailure(r5)
            r0 = r1
            r19 = r4
            r4 = r5
            r7 = r6
            r1 = r8
            r3 = r10
            r24 = 1
            r5 = r2
            r6 = r4
            r2 = r9
            goto L_0x010a
        L_0x0057:
            kotlin.ResultKt.throwOnFailure(r5)
            if (r2 != r11) goto L_0x0076
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r12 = r0.getMainUrl()
            java.lang.StringBuilder r7 = r7.append(r12)
            java.lang.StringBuilder r7 = r7.append(r10)
            java.lang.StringBuilder r7 = r7.append(r1)
            java.lang.String r7 = r7.toString()
            goto L_0x0097
        L_0x0076:
            java.lang.StringBuilder r7 = new java.lang.StringBuilder
            r7.<init>()
            java.lang.String r12 = r0.getMainUrl()
            java.lang.StringBuilder r7 = r7.append(r12)
            java.lang.StringBuilder r7 = r7.append(r9)
            java.lang.StringBuilder r7 = r7.append(r2)
            java.lang.StringBuilder r7 = r7.append(r10)
            java.lang.StringBuilder r7 = r7.append(r1)
            java.lang.String r7 = r7.toString()
        L_0x0097:
            java.lang.StringBuilder r12 = new java.lang.StringBuilder
            r12.<init>()
            java.lang.String r13 = "Log: Search Request - URL: "
            java.lang.StringBuilder r12 = r12.append(r13)
            java.lang.StringBuilder r12 = r12.append(r7)
            java.lang.String r12 = r12.toString()
            android.util.Log.d(r8, r12)
            r12 = r5
            com.lagradost.nicehttp.Requests r5 = com.lagradost.cloudstream3.MainActivityKt.getApp()
            com.kraptor.DiziPal$CloudflareInterceptor r13 = r0.getInterceptor()
            r16 = r13
            okhttp3.Interceptor r16 = (okhttp3.Interceptor) r16
            r4.L$0 = r1
            java.lang.Object r13 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r7)
            r4.L$1 = r13
            r4.I$0 = r2
            r4.label = r11
            r13 = r6
            r6 = r7
            r7 = 0
            r14 = r8
            r8 = 0
            r15 = r9
            r9 = 0
            r17 = r10
            r10 = 0
            r18 = 1
            r11 = 0
            r19 = r12
            r12 = 0
            r20 = r13
            r13 = 0
            r21 = r14
            r22 = r15
            r14 = 0
            r23 = r17
            r17 = 0
            r24 = 1
            r18 = 0
            r25 = r20
            r20 = 3582(0xdfe, float:5.02E-42)
            r26 = r21
            r21 = 0
            r0 = r19
            r19 = r4
            r4 = r0
            r2 = r22
            r3 = r23
            r0 = r25
            r1 = r26
            java.lang.Object r5 = com.lagradost.nicehttp.Requests.get$default(r5, r6, r7, r8, r9, r10, r11, r12, r13, r14, r16, r17, r18, r19, r20, r21)
            if (r5 != r0) goto L_0x0104
            return r0
        L_0x0104:
            r0 = r34
            r7 = r6
            r6 = r5
            r5 = r35
        L_0x010a:
            com.lagradost.nicehttp.NiceResponse r6 = (com.lagradost.nicehttp.NiceResponse) r6
            org.jsoup.nodes.Document r6 = r6.getDocument()
            java.util.ArrayList r8 = new java.util.ArrayList
            r8.<init>()
            java.lang.String r9 = "div.post-item"
            org.jsoup.select.Elements r9 = r6.select(r9)
            java.lang.Iterable r9 = (java.lang.Iterable) r9
            r10 = 0
            java.util.Iterator r11 = r9.iterator()
        L_0x0122:
            boolean r12 = r11.hasNext()
            if (r12 == 0) goto L_0x0232
            java.lang.Object r12 = r11.next()
            r14 = r12
            org.jsoup.nodes.Element r14 = (org.jsoup.nodes.Element) r14
            r15 = 0
            java.lang.String r13 = "a"
            org.jsoup.nodes.Element r13 = r14.selectFirst(r13)
            r16 = r4
            java.lang.String r4 = "img"
            r35 = r7
            if (r13 == 0) goto L_0x0172
            java.lang.String r7 = "title"
            java.lang.String r7 = r13.attr(r7)
            if (r7 == 0) goto L_0x0172
            java.lang.CharSequence r7 = (java.lang.CharSequence) r7
            int r18 = r7.length()
            if (r18 != 0) goto L_0x0151
            r18 = 1
            goto L_0x0153
        L_0x0151:
            r18 = 0
        L_0x0153:
            if (r18 == 0) goto L_0x016b
            r7 = 0
            r18 = r7
            org.jsoup.nodes.Element r7 = r14.selectFirst(r4)
            if (r7 == 0) goto L_0x0167
            r20 = r9
            java.lang.String r9 = "alt"
            java.lang.String r7 = r7.attr(r9)
            goto L_0x016d
        L_0x0167:
            r20 = r9
            r7 = 0
            goto L_0x016d
        L_0x016b:
            r20 = r9
        L_0x016d:
            java.lang.String r7 = (java.lang.String) r7
            if (r7 != 0) goto L_0x0176
            goto L_0x0174
        L_0x0172:
            r20 = r9
        L_0x0174:
            java.lang.String r7 = ""
        L_0x0176:
            r26 = r7
            r7 = r33
            com.lagradost.cloudstream3.MainAPI r7 = (com.lagradost.cloudstream3.MainAPI) r7
            if (r13 == 0) goto L_0x0185
            java.lang.String r9 = "href"
            java.lang.String r9 = r13.attr(r9)
            goto L_0x0186
        L_0x0185:
            r9 = 0
        L_0x0186:
            java.lang.String r27 = com.lagradost.cloudstream3.MainAPIKt.fixUrlNull(r7, r9)
            r7 = r33
            com.lagradost.cloudstream3.MainAPI r7 = (com.lagradost.cloudstream3.MainAPI) r7
            org.jsoup.nodes.Element r9 = r14.selectFirst(r4)
            if (r9 == 0) goto L_0x019f
            r18 = r10
            java.lang.String r10 = "data-src"
            java.lang.String r9 = r9.attr(r10)
            if (r9 != 0) goto L_0x01af
            goto L_0x01a1
        L_0x019f:
            r18 = r10
        L_0x01a1:
            org.jsoup.nodes.Element r4 = r14.selectFirst(r4)
            if (r4 == 0) goto L_0x01ae
            java.lang.String r9 = "src"
            java.lang.String r9 = r4.attr(r9)
            goto L_0x01af
        L_0x01ae:
            r9 = 0
        L_0x01af:
            java.lang.String r4 = com.lagradost.cloudstream3.MainAPIKt.fixUrlNull(r7, r9)
            r7 = r27
            java.lang.CharSequence r7 = (java.lang.CharSequence) r7
            if (r7 == 0) goto L_0x01c2
            int r7 = r7.length()
            if (r7 != 0) goto L_0x01c0
            goto L_0x01c2
        L_0x01c0:
            r7 = 0
            goto L_0x01c3
        L_0x01c2:
            r7 = 1
        L_0x01c3:
            if (r7 != 0) goto L_0x0221
            r7 = r26
            java.lang.CharSequence r7 = (java.lang.CharSequence) r7
            int r7 = r7.length()
            if (r7 <= 0) goto L_0x01d1
            r7 = 1
            goto L_0x01d2
        L_0x01d1:
            r7 = 0
        L_0x01d2:
            if (r7 == 0) goto L_0x0221
            r7 = r27
            java.lang.CharSequence r7 = (java.lang.CharSequence) r7
            java.lang.String r9 = "/dizi/"
            java.lang.CharSequence r9 = (java.lang.CharSequence) r9
            r10 = 2
            r21 = r11
            r34 = r12
            r11 = 0
            r12 = 0
            boolean r7 = kotlin.text.StringsKt.contains$default(r7, r9, r12, r10, r11)
            if (r7 == 0) goto L_0x0203
            r25 = r33
            com.lagradost.cloudstream3.MainAPI r25 = (com.lagradost.cloudstream3.MainAPI) r25
            com.lagradost.cloudstream3.TvType r28 = com.lagradost.cloudstream3.TvType.TvSeries
            com.kraptor.DiziPal$$ExternalSyntheticLambda0 r7 = new com.kraptor.DiziPal$$ExternalSyntheticLambda0
            r7.<init>(r4)
            r31 = 8
            r32 = 0
            r29 = 0
            r30 = r7
            com.lagradost.cloudstream3.TvSeriesSearchResponse r7 = com.lagradost.cloudstream3.MainAPIKt.newTvSeriesSearchResponse$default(r25, r26, r27, r28, r29, r30, r31, r32)
            com.lagradost.cloudstream3.SearchResponse r7 = (com.lagradost.cloudstream3.SearchResponse) r7
            goto L_0x021c
        L_0x0203:
            r25 = r33
            com.lagradost.cloudstream3.MainAPI r25 = (com.lagradost.cloudstream3.MainAPI) r25
            com.lagradost.cloudstream3.TvType r28 = com.lagradost.cloudstream3.TvType.Movie
            com.kraptor.DiziPal$$ExternalSyntheticLambda1 r7 = new com.kraptor.DiziPal$$ExternalSyntheticLambda1
            r7.<init>(r4)
            r31 = 8
            r32 = 0
            r29 = 0
            r30 = r7
            com.lagradost.cloudstream3.MovieSearchResponse r7 = com.lagradost.cloudstream3.MainAPIKt.newMovieSearchResponse$default(r25, r26, r27, r28, r29, r30, r31, r32)
            com.lagradost.cloudstream3.SearchResponse r7 = (com.lagradost.cloudstream3.SearchResponse) r7
        L_0x021c:
            r8.add(r7)
            goto L_0x0225
        L_0x0221:
            r21 = r11
            r34 = r12
        L_0x0225:
            r7 = r35
            r4 = r16
            r10 = r18
            r9 = r20
            r11 = r21
            goto L_0x0122
        L_0x0232:
            r16 = r4
            r35 = r7
            r20 = r9
            r18 = r10
            r12 = 0
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r7 = r33.getMainUrl()
            java.lang.StringBuilder r4 = r4.append(r7)
            java.lang.StringBuilder r2 = r4.append(r2)
            int r4 = r5 + 1
            java.lang.StringBuilder r2 = r2.append(r4)
            java.lang.StringBuilder r2 = r2.append(r3)
            java.lang.StringBuilder r2 = r2.append(r0)
            java.lang.String r2 = r2.toString()
            java.lang.StringBuilder r3 = new java.lang.StringBuilder
            r3.<init>()
            java.lang.String r4 = "a[href*='/page/"
            java.lang.StringBuilder r3 = r3.append(r4)
            int r4 = r5 + 1
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.String r4 = "/']"
            java.lang.StringBuilder r3 = r3.append(r4)
            java.lang.String r3 = r3.toString()
            org.jsoup.select.Elements r3 = r6.select(r3)
            java.util.Collection r3 = (java.util.Collection) r3
            boolean r3 = r3.isEmpty()
            r3 = r3 ^ 1
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            java.lang.String r7 = "Log: Page Check - Current: "
            java.lang.StringBuilder r4 = r4.append(r7)
            java.lang.StringBuilder r4 = r4.append(r5)
            java.lang.String r7 = ", Next Target: "
            java.lang.StringBuilder r4 = r4.append(r7)
            java.lang.StringBuilder r4 = r4.append(r2)
            java.lang.String r7 = ", Found: "
            java.lang.StringBuilder r4 = r4.append(r7)
            if (r3 == 0) goto L_0x02a8
            r11 = 1
            goto L_0x02a9
        L_0x02a8:
            r11 = 0
        L_0x02a9:
            java.lang.StringBuilder r4 = r4.append(r11)
            java.lang.String r4 = r4.toString()
            android.util.Log.d(r1, r4)
            r1 = r8
            java.util.List r1 = (java.util.List) r1
            if (r3 == 0) goto L_0x02bb
            r11 = 1
            goto L_0x02bc
        L_0x02bb:
            r11 = 0
        L_0x02bc:
            java.lang.Boolean r4 = kotlin.coroutines.jvm.internal.Boxing.boxBoolean(r11)
            com.lagradost.cloudstream3.SearchResponseList r1 = com.lagradost.cloudstream3.MainAPIKt.newSearchResponseList(r1, r4)
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: com.kraptor.DiziPal.search(java.lang.String, int, kotlin.coroutines.Continuation):java.lang.Object");
    }

    /* access modifiers changed from: private */
    public static final Unit search$lambda$0$1(String $posterUrl, TvSeriesSearchResponse $this$newTvSeriesSearchResponse) {
        $this$newTvSeriesSearchResponse.setPosterUrl($posterUrl);
        return Unit.INSTANCE;
    }

    /* access modifiers changed from: private */
    public static final Unit search$lambda$0$2(String $posterUrl, MovieSearchResponse $this$newMovieSearchResponse) {
        $this$newMovieSearchResponse.setPosterUrl($posterUrl);
        return Unit.INSTANCE;
    }

    @Nullable
    public Object quickSearch(@NotNull String query, @NotNull Continuation<? super List<? extends SearchResponse>> $completion) {
        return search(query, $completion);
    }

    /* JADX WARNING: Code restructure failed: missing block: B:100:0x03c6, code lost:
        r21 = r2;
        r2 = (org.jsoup.nodes.Element) r12.next();
        r29 = r3;
        r30 = (org.jsoup.nodes.Element) kotlin.collections.CollectionsKt.firstOrNull(r2.select("h4"));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:101:0x03e1, code lost:
        if (r30 == null) goto L_0x03f4;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:102:0x03e3, code lost:
        r30 = r30.text();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:103:0x03e7, code lost:
        if (r30 == null) goto L_0x03f4;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:104:0x03e9, code lost:
        r30 = kotlin.text.StringsKt.trim(r30).toString();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:105:0x03f4, code lost:
        r30 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:106:0x03f6, code lost:
        r31 = r6;
        r6 = java.lang.String.valueOf(r30);
        r3 = (org.jsoup.nodes.Element) kotlin.collections.CollectionsKt.lastOrNull(r2.select("h4"));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:107:0x0408, code lost:
        if (r3 == null) goto L_0x041b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:108:0x040a, code lost:
        r3 = r3.text();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:109:0x040e, code lost:
        if (r3 == null) goto L_0x041b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:110:0x0410, code lost:
        r3 = kotlin.text.StringsKt.trim(r3).toString();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:111:0x041b, code lost:
        r3 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:112:0x041c, code lost:
        r30 = r7;
        r7 = (com.lagradost.cloudstream3.MainAPI) r0;
        r32 = r8;
        r8 = r2.selectFirst("img");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:113:0x0429, code lost:
        if (r8 == null) goto L_0x0430;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:114:0x042b, code lost:
        r8 = r8.attr("src");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:115:0x0430, code lost:
        r8 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:116:0x0431, code lost:
        r5.add(kotlin.TuplesKt.to(new com.lagradost.cloudstream3.Actor(r6, java.lang.String.valueOf(com.lagradost.cloudstream3.MainAPIKt.fixUrlNull(r7, r8))), r3));
        r2 = r21;
        r3 = r29;
        r7 = r30;
        r6 = r31;
        r8 = r32;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:117:0x0451, code lost:
        r21 = r2;
        r29 = r3;
        r31 = r6;
        r30 = r7;
        r32 = r8;
        r2 = (java.util.List) r5;
        r3 = r2.isEmpty();
        r2 = r2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:118:0x0465, code lost:
        if (r3 == false) goto L_0x04c6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:119:0x0467, code lost:
        r2 = 0;
        r3 = r10.select("span:contains(Oyuncular) + div a");
        r4 = false;
        r5 = new java.util.ArrayList(kotlin.collections.CollectionsKt.collectionSizeOrDefault(r3, 10));
        r8 = r3.iterator();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:121:0x0488, code lost:
        if (r8.hasNext() == false) goto L_0x04ba;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:122:0x048a, code lost:
        r5.add(kotlin.TuplesKt.to(new com.lagradost.cloudstream3.Actor(kotlin.text.StringsKt.trim(((org.jsoup.nodes.Element) r8.next()).text()).toString(), (java.lang.String) null), (java.lang.Object) null));
        r3 = r3;
        r2 = r2;
        r4 = r4;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:123:0x04ba, code lost:
        r28 = r2;
        r21 = r3;
        r29 = r4;
        r2 = (java.util.List) r5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:124:0x04c6, code lost:
        r28 = (java.util.List) r2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:125:0x04da, code lost:
        if (kotlin.text.StringsKt.contains$default(r19, "/anime/", false, 2, (java.lang.Object) null) != false) goto L_0x04ed;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:127:0x04e8, code lost:
        if (kotlin.text.StringsKt.contains$default(r19, "/animeler/", false, 2, (java.lang.Object) null) == false) goto L_0x04eb;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:128:0x04eb, code lost:
        r5 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:129:0x04ed, code lost:
        r5 = 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:130:0x04ee, code lost:
        r2 = r5;
        r11 = kotlin.text.StringsKt.contains$default(r19, "/dizi/", false, 2, (java.lang.Object) null);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:131:0x04fe, code lost:
        if (r2 != 0) goto L_0x05b8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:132:0x0500, code lost:
        if (r11 == false) goto L_0x051c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:133:0x0502, code lost:
        r21 = r13;
        r4 = r18;
        r5 = r19;
        r13 = r22;
        r14 = r27;
        r12 = r30;
        r22 = r2;
        r19 = r11;
        r18 = r15;
        r15 = r23;
        r11 = r24;
        r2 = r31;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:134:0x051c, code lost:
        r14 = r27;
        r9.L$0 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r19);
        r9.L$1 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r10);
        r9.L$2 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r15);
        r9.L$3 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r22);
        r9.L$4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r13);
        r9.L$5 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r18);
        r9.L$6 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r23);
        r9.L$7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r24);
        r9.L$8 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r31);
        r9.L$9 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r30);
        r9.L$10 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r14);
        r9.L$11 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r28);
        r9.I$0 = r2;
        r9.Z$0 = r11;
        r9.label = 5;
        r7 = r19;
        r21 = r13;
        r4 = r18;
        r5 = r19;
        r13 = r22;
        r12 = r30;
        r22 = r2;
        r19 = r11;
        r18 = r15;
        r15 = r23;
        r11 = r24;
        r2 = r31;
        r3 = com.lagradost.cloudstream3.MainAPIKt.newMovieLoadResponse((com.lagradost.cloudstream3.MainAPI) r0, r4, r5, com.lagradost.cloudstream3.TvType.Movie, r7, new com.kraptor.DiziPal$load$4(r22, r23, r24, r31, r30, r27, r28, (kotlin.coroutines.Continuation<? super com.kraptor.DiziPal$load$4>) null), r9);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:135:0x059f, code lost:
        if (r3 != r1) goto L_0x05a2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:136:0x05a1, code lost:
        return r1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:137:0x05a2, code lost:
        r8 = r2;
        r2 = r3;
        r1 = r5;
        r16 = r10;
        r10 = r11;
        r7 = r12;
        r6 = r14;
        r11 = r15;
        r15 = r18;
        r3 = r19;
        r5 = r28;
        r12 = r4;
        r14 = r13;
        r13 = r21;
        r4 = r22;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:138:0x05b8, code lost:
        r21 = r13;
        r4 = r18;
        r5 = r19;
        r13 = r22;
        r14 = r27;
        r12 = r30;
        r22 = r2;
        r19 = r11;
        r18 = r15;
        r15 = r23;
        r11 = r24;
        r2 = r31;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:139:0x05d0, code lost:
        r6 = r10.select("div#season-options-list li a:not(.font-bold)");
        r3 = load$parseEpisodes(r0, r10, 1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:140:0x05e5, code lost:
        if (r6.isEmpty() != false) goto L_0x06b6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:141:0x05e7, code lost:
        r7 = r6;
        r20 = r6;
        r6 = new java.util.ArrayList(kotlin.collections.CollectionsKt.collectionSizeOrDefault(r7, 10));
        r8 = r7;
        r26 = r8.iterator();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:143:0x0607, code lost:
        if (r26.hasNext() == false) goto L_0x062c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:144:0x0609, code lost:
        r6.add(new com.kraptor.DiziPal$load$otherSeasonsEpisodes$1$1(r0, (org.jsoup.nodes.Element) r26.next(), (kotlin.coroutines.Continuation<? super com.kraptor.DiziPal$load$otherSeasonsEpisodes$1$1>) null));
        r7 = r7;
        r8 = r8;
        r10 = r10;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:145:0x062c, code lost:
        r29 = r7;
        r31 = r8;
        r32 = r10;
        r6 = (kotlin.jvm.functions.Function1[]) ((java.util.List) r6).toArray(new kotlin.jvm.functions.Function1[0]);
        r9.L$0 = r5;
        r9.L$1 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r32);
        r9.L$2 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r18);
        r9.L$3 = r13;
        r9.L$4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r21);
        r9.L$5 = r4;
        r9.L$6 = r15;
        r9.L$7 = r11;
        r9.L$8 = r2;
        r9.L$9 = r12;
        r9.L$10 = r14;
        r7 = r28;
        r9.L$11 = r7;
        r9.L$12 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r20);
        r9.L$13 = r3;
        r8 = r22;
        r9.I$0 = r8;
        r10 = r19;
        r9.Z$0 = r10;
        r9.label = 2;
        r0 = com.lagradost.cloudstream3.ParCollectionsKt.runAllAsync((kotlin.jvm.functions.Function1[]) java.util.Arrays.copyOf(r6, r6.length), r9);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:146:0x0687, code lost:
        if (r0 != r1) goto L_0x068a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:147:0x0689, code lost:
        return r1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:148:0x068a, code lost:
        r6 = r14;
        r14 = r4;
        r4 = r8;
        r8 = r6;
        r16 = r10;
        r10 = r12;
        r6 = r20;
        r12 = r11;
        r11 = r2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:149:0x0695, code lost:
        r0 = kotlin.collections.CollectionsKt.flatten(kotlin.collections.CollectionsKt.filterNotNull((java.lang.Iterable) r0));
        r2 = r4;
        r20 = r6;
        r27 = r10;
        r26 = r11;
        r25 = r12;
        r4 = r14;
        r10 = r16;
        r14 = r8;
        r29 = r7;
        r23 = r13;
        r24 = r15;
        r11 = r3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:150:0x06b6, code lost:
        r20 = r6;
        r32 = r10;
        r10 = r19;
        r0 = kotlin.collections.CollectionsKt.emptyList();
        r26 = r2;
        r2 = r22;
        r25 = r11;
        r27 = r12;
        r29 = r28;
        r23 = r13;
        r24 = r15;
        r11 = r3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:151:0x06d2, code lost:
        r7 = kotlin.collections.CollectionsKt.plus(r11, r0);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:152:0x06dd, code lost:
        if (r2 == 0) goto L_0x077b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:153:0x06df, code lost:
        r28 = r7;
        r12 = r28;
        r9.L$0 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r5);
        r9.L$1 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r32);
        r9.L$2 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r18);
        r9.L$3 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r23);
        r9.L$4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r21);
        r9.L$5 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r4);
        r9.L$6 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r24);
        r9.L$7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r25);
        r9.L$8 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r26);
        r9.L$9 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r27);
        r9.L$10 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r14);
        r9.L$11 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r29);
        r9.L$12 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r20);
        r9.L$13 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r11);
        r9.L$14 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r0);
        r9.L$15 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r12);
        r9.I$0 = r2;
        r9.Z$0 = r10;
        r9.label = 3;
        r3 = com.lagradost.cloudstream3.MainAPIKt.newAnimeLoadResponse((com.lagradost.cloudstream3.MainAPI) r33, r4, r5, com.lagradost.cloudstream3.TvType.Anime, false, new com.kraptor.DiziPal$load$2(r23, r24, r25, r26, r27, r28, r29, (kotlin.coroutines.Continuation<? super com.kraptor.DiziPal$load$2>) null), r9);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:154:0x0760, code lost:
        if (r3 != r1) goto L_0x0763;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:155:0x0762, code lost:
        return r1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:156:0x0763, code lost:
        r15 = r4;
        r16 = r10;
        r6 = r11;
        r4 = r12;
        r10 = r14;
        r7 = r20;
        r1 = r21;
        r14 = r24;
        r13 = r25;
        r12 = r26;
        r11 = r27;
        r8 = r29;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:158:0x077b, code lost:
        r12 = r7;
        r28 = r14;
        r9.L$0 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r5);
        r9.L$1 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r32);
        r9.L$2 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r18);
        r9.L$3 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r23);
        r9.L$4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r21);
        r9.L$5 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r4);
        r9.L$6 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r24);
        r9.L$7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r25);
        r9.L$8 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r26);
        r9.L$9 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r27);
        r9.L$10 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r28);
        r9.L$11 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r29);
        r9.L$12 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r20);
        r9.L$13 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r11);
        r9.L$14 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r0);
        r9.L$15 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r12);
        r9.I$0 = r2;
        r9.Z$0 = r10;
        r9.label = 4;
        r7 = r12;
        r3 = com.lagradost.cloudstream3.MainAPIKt.newTvSeriesLoadResponse((com.lagradost.cloudstream3.MainAPI) r33, r4, r5, com.lagradost.cloudstream3.TvType.TvSeries, r7, new com.kraptor.DiziPal$load$3(r23, r24, r25, r26, r27, r28, r29, (kotlin.coroutines.Continuation<? super com.kraptor.DiziPal$load$3>) null), r9);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:159:0x07fb, code lost:
        if (r3 != r1) goto L_0x07fe;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:160:0x07fd, code lost:
        return r1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:161:0x07fe, code lost:
        r15 = r4;
        r4 = r7;
        r16 = r10;
        r6 = r11;
        r7 = r20;
        r1 = r21;
        r14 = r24;
        r13 = r25;
        r12 = r26;
        r11 = r27;
        r10 = r28;
        r8 = r29;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:163:0x0816, code lost:
        r17 = r2;
        r32 = r10;
        r18 = r15;
        r13 = r22;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:171:?, code lost:
        return r2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:173:?, code lost:
        return (com.lagradost.cloudstream3.LoadResponse) r3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:174:?, code lost:
        return (com.lagradost.cloudstream3.LoadResponse) r3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:175:?, code lost:
        return null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:176:?, code lost:
        return null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x01d6, code lost:
        r10 = ((com.lagradost.nicehttp.NiceResponse) r3).getDocument();
        r15 = r10.selectFirst("div.poster img");
        r3 = (com.lagradost.cloudstream3.MainAPI) r0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x01e7, code lost:
        if (r15 == null) goto L_0x0205;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x01e9, code lost:
        r8 = r15.attr("data-src");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x01ef, code lost:
        if (r8 == null) goto L_0x0205;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:24:0x01fa, code lost:
        if (r8.length() <= 0) goto L_0x01fe;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:25:0x01fc, code lost:
        r13 = true;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:26:0x01fe, code lost:
        r13 = false;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x01ff, code lost:
        if (r13 == false) goto L_0x0202;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0202, code lost:
        r8 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:29:0x0203, code lost:
        if (r8 != null) goto L_0x022f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:30:0x0205, code lost:
        if (r15 == null) goto L_0x020c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:31:0x0207, code lost:
        r8 = r15.attr("src");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:32:0x020c, code lost:
        r8 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:33:0x020d, code lost:
        if (r8 != null) goto L_0x022f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:34:0x020f, code lost:
        r8 = r10.selectFirst("div.before-video-player img");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:35:0x0215, code lost:
        if (r8 == null) goto L_0x021c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:36:0x0217, code lost:
        r8 = r8.attr("src");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:37:0x021c, code lost:
        r8 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:38:0x021d, code lost:
        if (r8 != null) goto L_0x022f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:39:0x021f, code lost:
        r8 = r10.selectFirst("meta[property='og:image']");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:40:0x0225, code lost:
        if (r8 == null) goto L_0x022e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x0227, code lost:
        r8 = r8.attr("content");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:42:0x022e, code lost:
        r8 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:43:0x022f, code lost:
        r22 = com.lagradost.cloudstream3.MainAPIKt.fixUrlNull(r3, r8);
        r3 = r10.selectFirst("h1");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:44:0x0239, code lost:
        if (r3 == null) goto L_0x0816;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:45:0x023b, code lost:
        r3 = r3.text();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:46:0x023f, code lost:
        if (r3 != null) goto L_0x024b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:47:0x0241, code lost:
        r17 = r2;
        r32 = r10;
        r18 = r15;
        r13 = r22;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:48:0x024b, code lost:
        r13 = r3;
        r3 = kotlin.text.StringsKt.trim(new kotlin.text.Regex("\\(.*\\)").replace(r13, "")).toString();
        r8 = r10.selectFirst("h1 a");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:49:0x026c, code lost:
        if (r8 == null) goto L_0x027e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:50:0x026e, code lost:
        r8 = r8.text();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:51:0x0272, code lost:
        if (r8 == null) goto L_0x027e;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:52:0x0274, code lost:
        r8 = kotlin.text.StringsKt.toIntOrNull(r8);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:53:0x0278, code lost:
        if (r8 != null) goto L_0x027b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:54:0x027b, code lost:
        r23 = r8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:55:0x027e, code lost:
        r8 = load$getDataByLabel(r10, "Yapım Yılı");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:56:0x0284, code lost:
        if (r8 == null) goto L_0x028d;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:57:0x0286, code lost:
        r23 = kotlin.text.StringsKt.toIntOrNull(r8);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:58:0x028d, code lost:
        r23 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:59:0x028f, code lost:
        r8 = r10.selectFirst("p.summary-text");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:60:0x0296, code lost:
        if (r8 == null) goto L_0x02ae;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:61:0x0298, code lost:
        r8 = r8.text();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:62:0x029c, code lost:
        if (r8 == null) goto L_0x02ae;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:63:0x029e, code lost:
        r8 = kotlin.text.StringsKt.trim(r8).toString();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:64:0x02a8, code lost:
        if (r8 != null) goto L_0x02ab;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:65:0x02ab, code lost:
        r24 = r8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:66:0x02ae, code lost:
        r8 = r10.selectFirst("h6:contains(Film Özeti) + p");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:67:0x02b4, code lost:
        if (r8 == null) goto L_0x02c9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:68:0x02b6, code lost:
        r8 = r8.text();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:69:0x02ba, code lost:
        if (r8 == null) goto L_0x02c9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:70:0x02bc, code lost:
        r24 = kotlin.text.StringsKt.trim(r8).toString();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:71:0x02c9, code lost:
        r24 = load$getDataByLabel(r10, "Film Özeti");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:72:0x02d2, code lost:
        r8 = r10.select("section h1 + span + ul a, section h1 + ul li a");
        r6 = new java.util.ArrayList(kotlin.collections.CollectionsKt.collectionSizeOrDefault(r8, 10));
        r17 = r8.iterator();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:74:0x02f5, code lost:
        if (r17.hasNext() == false) goto L_0x0309;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:75:0x02f7, code lost:
        r6.add(((org.jsoup.nodes.Element) r17.next()).text());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:76:0x0309, code lost:
        r6 = (java.util.List) r6;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:77:0x0313, code lost:
        if (r6.isEmpty() == false) goto L_0x0350;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:78:0x0315, code lost:
        r7 = r10.select("div:has(> span:contains(Tür)) > div a");
        r11 = new java.util.ArrayList(kotlin.collections.CollectionsKt.collectionSizeOrDefault(r7, 10));
        r17 = r7.iterator();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:80:0x0335, code lost:
        if (r17.hasNext() == false) goto L_0x034b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:81:0x0337, code lost:
        r11.add(((org.jsoup.nodes.Element) r17.next()).text());
     */
    /* JADX WARNING: Code restructure failed: missing block: B:82:0x034b, code lost:
        r6 = (java.util.List) r11;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:83:0x0350, code lost:
        r6 = (java.util.List) r6;
        r27 = load$getDataByLabel(r10, "IMDB Puanı");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:84:0x0359, code lost:
        if (r27 == null) goto L_0x036a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:85:0x035b, code lost:
        r7 = kotlin.text.StringsKt.replace$default(r27, ",", ".", false, 4, (java.lang.Object) null);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:86:0x036a, code lost:
        r7 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:87:0x036b, code lost:
        r8 = load$getDataByLabel(r10, "Süre");
     */
    /* JADX WARNING: Code restructure failed: missing block: B:88:0x0371, code lost:
        if (r8 == null) goto L_0x039c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:89:0x0373, code lost:
        r17 = r2;
        r18 = r3;
        r19 = r5;
        r11 = kotlin.text.Regex.find$default(new kotlin.text.Regex("\\d+"), r8, 0, 2, (java.lang.Object) null);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:90:0x038b, code lost:
        if (r11 == null) goto L_0x0398;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:91:0x038d, code lost:
        r2 = r11.getValue();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:92:0x0391, code lost:
        if (r2 == null) goto L_0x0398;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:93:0x0393, code lost:
        r2 = kotlin.text.StringsKt.toIntOrNull(r2);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:94:0x0398, code lost:
        r2 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:95:0x0399, code lost:
        r27 = r2;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:96:0x039c, code lost:
        r17 = r2;
        r18 = r3;
        r19 = r5;
        r27 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:97:0x03a4, code lost:
        r2 = r10.select("div.swiper-slide");
        r3 = false;
        r5 = new java.util.ArrayList(kotlin.collections.CollectionsKt.collectionSizeOrDefault(r2, 10));
        r8 = r2;
        r12 = r8.iterator();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:99:0x03c4, code lost:
        if (r12.hasNext() == false) goto L_0x0451;
     */
    /* JADX WARNING: Removed duplicated region for block: B:10:0x0032  */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x006f  */
    /* JADX WARNING: Removed duplicated region for block: B:12:0x00d1  */
    /* JADX WARNING: Removed duplicated region for block: B:13:0x0133  */
    /* JADX WARNING: Removed duplicated region for block: B:14:0x018a  */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x0195  */
    /* JADX WARNING: Removed duplicated region for block: B:8:0x002a  */
    @org.jetbrains.annotations.Nullable
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public java.lang.Object load(@org.jetbrains.annotations.NotNull java.lang.String r34, @org.jetbrains.annotations.NotNull kotlin.coroutines.Continuation<? super com.lagradost.cloudstream3.LoadResponse> r35) {
        /*
            r33 = this;
            r0 = r33
            r1 = r35
            boolean r2 = r1 instanceof com.kraptor.DiziPal$load$1
            if (r2 == 0) goto L_0x0018
            r2 = r1
            com.kraptor.DiziPal$load$1 r2 = (com.kraptor.DiziPal$load$1) r2
            int r3 = r2.label
            r4 = -2147483648(0xffffffff80000000, float:-0.0)
            r3 = r3 & r4
            if (r3 == 0) goto L_0x0018
            int r3 = r2.label
            int r3 = r3 - r4
            r2.label = r3
            goto L_0x001d
        L_0x0018:
            com.kraptor.DiziPal$load$1 r2 = new com.kraptor.DiziPal$load$1
            r2.<init>(r0, r1)
        L_0x001d:
            r9 = r2
            java.lang.Object r2 = r9.result
            java.lang.Object r3 = kotlin.coroutines.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED()
            int r4 = r9.label
            r5 = 1
            switch(r4) {
                case 0: goto L_0x0195;
                case 1: goto L_0x018a;
                case 2: goto L_0x0133;
                case 3: goto L_0x00d1;
                case 4: goto L_0x006f;
                case 5: goto L_0x0032;
                default: goto L_0x002a;
            }
        L_0x002a:
            java.lang.IllegalStateException r0 = new java.lang.IllegalStateException
            java.lang.String r1 = "call to 'resume' before 'invoke' with coroutine"
            r0.<init>(r1)
            throw r0
        L_0x0032:
            boolean r3 = r9.Z$0
            int r4 = r9.I$0
            java.lang.Object r5 = r9.L$11
            java.util.List r5 = (java.util.List) r5
            java.lang.Object r6 = r9.L$10
            java.lang.Integer r6 = (java.lang.Integer) r6
            java.lang.Object r7 = r9.L$9
            java.lang.String r7 = (java.lang.String) r7
            java.lang.Object r8 = r9.L$8
            java.util.List r8 = (java.util.List) r8
            java.lang.Object r10 = r9.L$7
            java.lang.String r10 = (java.lang.String) r10
            java.lang.Object r11 = r9.L$6
            java.lang.Integer r11 = (java.lang.Integer) r11
            java.lang.Object r12 = r9.L$5
            java.lang.String r12 = (java.lang.String) r12
            java.lang.Object r13 = r9.L$4
            java.lang.String r13 = (java.lang.String) r13
            java.lang.Object r14 = r9.L$3
            java.lang.String r14 = (java.lang.String) r14
            java.lang.Object r15 = r9.L$2
            org.jsoup.nodes.Element r15 = (org.jsoup.nodes.Element) r15
            java.lang.Object r1 = r9.L$1
            org.jsoup.nodes.Document r1 = (org.jsoup.nodes.Document) r1
            r16 = r1
            java.lang.Object r1 = r9.L$0
            java.lang.String r1 = (java.lang.String) r1
            kotlin.ResultKt.throwOnFailure(r2)
            r17 = r2
            goto L_0x05b7
        L_0x006f:
            boolean r1 = r9.Z$0
            int r3 = r9.I$0
            java.lang.Object r4 = r9.L$15
            java.util.List r4 = (java.util.List) r4
            java.lang.Object r5 = r9.L$14
            java.util.List r5 = (java.util.List) r5
            java.lang.Object r6 = r9.L$13
            java.util.List r6 = (java.util.List) r6
            java.lang.Object r7 = r9.L$12
            org.jsoup.select.Elements r7 = (org.jsoup.select.Elements) r7
            java.lang.Object r8 = r9.L$11
            java.util.List r8 = (java.util.List) r8
            java.lang.Object r10 = r9.L$10
            java.lang.Integer r10 = (java.lang.Integer) r10
            java.lang.Object r11 = r9.L$9
            java.lang.String r11 = (java.lang.String) r11
            java.lang.Object r12 = r9.L$8
            java.util.List r12 = (java.util.List) r12
            java.lang.Object r13 = r9.L$7
            java.lang.String r13 = (java.lang.String) r13
            java.lang.Object r14 = r9.L$6
            java.lang.Integer r14 = (java.lang.Integer) r14
            java.lang.Object r15 = r9.L$5
            java.lang.String r15 = (java.lang.String) r15
            r16 = r1
            java.lang.Object r1 = r9.L$4
            java.lang.String r1 = (java.lang.String) r1
            r17 = r1
            java.lang.Object r1 = r9.L$3
            java.lang.String r1 = (java.lang.String) r1
            r18 = r1
            java.lang.Object r1 = r9.L$2
            org.jsoup.nodes.Element r1 = (org.jsoup.nodes.Element) r1
            r19 = r1
            java.lang.Object r1 = r9.L$1
            org.jsoup.nodes.Document r1 = (org.jsoup.nodes.Document) r1
            r20 = r1
            java.lang.Object r1 = r9.L$0
            java.lang.String r1 = (java.lang.String) r1
            kotlin.ResultKt.throwOnFailure(r2)
            r0 = r5
            r23 = r18
            r18 = r19
            r32 = r20
            r5 = r1
            r1 = r17
            r17 = r2
            r2 = r3
            r3 = r17
            goto L_0x0813
        L_0x00d1:
            boolean r1 = r9.Z$0
            int r3 = r9.I$0
            java.lang.Object r4 = r9.L$15
            java.util.List r4 = (java.util.List) r4
            java.lang.Object r5 = r9.L$14
            java.util.List r5 = (java.util.List) r5
            java.lang.Object r6 = r9.L$13
            java.util.List r6 = (java.util.List) r6
            java.lang.Object r7 = r9.L$12
            org.jsoup.select.Elements r7 = (org.jsoup.select.Elements) r7
            java.lang.Object r8 = r9.L$11
            java.util.List r8 = (java.util.List) r8
            java.lang.Object r10 = r9.L$10
            java.lang.Integer r10 = (java.lang.Integer) r10
            java.lang.Object r11 = r9.L$9
            java.lang.String r11 = (java.lang.String) r11
            java.lang.Object r12 = r9.L$8
            java.util.List r12 = (java.util.List) r12
            java.lang.Object r13 = r9.L$7
            java.lang.String r13 = (java.lang.String) r13
            java.lang.Object r14 = r9.L$6
            java.lang.Integer r14 = (java.lang.Integer) r14
            java.lang.Object r15 = r9.L$5
            java.lang.String r15 = (java.lang.String) r15
            r16 = r1
            java.lang.Object r1 = r9.L$4
            java.lang.String r1 = (java.lang.String) r1
            r17 = r1
            java.lang.Object r1 = r9.L$3
            java.lang.String r1 = (java.lang.String) r1
            r18 = r1
            java.lang.Object r1 = r9.L$2
            org.jsoup.nodes.Element r1 = (org.jsoup.nodes.Element) r1
            r19 = r1
            java.lang.Object r1 = r9.L$1
            org.jsoup.nodes.Document r1 = (org.jsoup.nodes.Document) r1
            r20 = r1
            java.lang.Object r1 = r9.L$0
            java.lang.String r1 = (java.lang.String) r1
            kotlin.ResultKt.throwOnFailure(r2)
            r0 = r5
            r23 = r18
            r18 = r19
            r32 = r20
            r5 = r1
            r1 = r17
            r17 = r2
            r2 = r3
            r3 = r17
            goto L_0x0777
        L_0x0133:
            boolean r1 = r9.Z$0
            int r4 = r9.I$0
            java.lang.Object r5 = r9.L$13
            java.util.List r5 = (java.util.List) r5
            java.lang.Object r6 = r9.L$12
            org.jsoup.select.Elements r6 = (org.jsoup.select.Elements) r6
            java.lang.Object r7 = r9.L$11
            java.util.List r7 = (java.util.List) r7
            java.lang.Object r8 = r9.L$10
            java.lang.Integer r8 = (java.lang.Integer) r8
            java.lang.Object r10 = r9.L$9
            java.lang.String r10 = (java.lang.String) r10
            java.lang.Object r11 = r9.L$8
            java.util.List r11 = (java.util.List) r11
            java.lang.Object r12 = r9.L$7
            java.lang.String r12 = (java.lang.String) r12
            java.lang.Object r13 = r9.L$6
            java.lang.Integer r13 = (java.lang.Integer) r13
            java.lang.Object r14 = r9.L$5
            java.lang.String r14 = (java.lang.String) r14
            java.lang.Object r15 = r9.L$4
            java.lang.String r15 = (java.lang.String) r15
            r16 = r1
            java.lang.Object r1 = r9.L$3
            java.lang.String r1 = (java.lang.String) r1
            r17 = r1
            java.lang.Object r1 = r9.L$2
            org.jsoup.nodes.Element r1 = (org.jsoup.nodes.Element) r1
            r18 = r1
            java.lang.Object r1 = r9.L$1
            org.jsoup.nodes.Document r1 = (org.jsoup.nodes.Document) r1
            r19 = r1
            java.lang.Object r1 = r9.L$0
            java.lang.String r1 = (java.lang.String) r1
            kotlin.ResultKt.throwOnFailure(r2)
            r0 = r5
            r5 = r1
            r1 = r3
            r3 = r0
            r0 = r2
            r21 = r15
            r32 = r19
            r15 = r13
            r13 = r17
            r17 = r0
            goto L_0x0695
        L_0x018a:
            java.lang.Object r1 = r9.L$0
            java.lang.String r1 = (java.lang.String) r1
            kotlin.ResultKt.throwOnFailure(r2)
            r5 = r1
            r1 = r3
            r3 = r2
            goto L_0x01d6
        L_0x0195:
            kotlin.ResultKt.throwOnFailure(r2)
            r1 = r3
            com.lagradost.nicehttp.Requests r3 = com.lagradost.cloudstream3.MainActivityKt.getApp()
            com.kraptor.DiziPal$CloudflareInterceptor r4 = r0.getInterceptor()
            r14 = r4
            okhttp3.Interceptor r14 = (okhttp3.Interceptor) r14
            r4 = r34
            r9.L$0 = r4
            r9.label = r5
            r6 = 1
            r5 = 0
            r7 = 1
            r6 = 0
            r8 = 1
            r7 = 0
            r10 = 1
            r8 = 0
            r17 = r9
            r9 = 0
            r11 = 1
            r10 = 0
            r12 = 1
            r11 = 0
            r15 = 1
            r12 = 0
            r16 = 1
            r15 = 0
            r18 = 1
            r16 = 0
            r19 = 1
            r18 = 3582(0xdfe, float:5.02E-42)
            r20 = 1
            r19 = 0
            java.lang.Object r3 = com.lagradost.nicehttp.Requests.get$default(r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r14, r15, r16, r17, r18, r19)
            r9 = r17
            if (r3 != r1) goto L_0x01d4
            return r1
        L_0x01d4:
            r5 = r34
        L_0x01d6:
            com.lagradost.nicehttp.NiceResponse r3 = (com.lagradost.nicehttp.NiceResponse) r3
            org.jsoup.nodes.Document r10 = r3.getDocument()
            java.lang.String r3 = "div.poster img"
            org.jsoup.nodes.Element r15 = r10.selectFirst(r3)
            r3 = r0
            com.lagradost.cloudstream3.MainAPI r3 = (com.lagradost.cloudstream3.MainAPI) r3
            java.lang.String r4 = "src"
            if (r15 == 0) goto L_0x0205
            java.lang.String r8 = "data-src"
            java.lang.String r8 = r15.attr(r8)
            if (r8 == 0) goto L_0x0205
            r11 = r8
            r12 = 0
            r13 = r11
            java.lang.CharSequence r13 = (java.lang.CharSequence) r13
            int r13 = r13.length()
            if (r13 <= 0) goto L_0x01fe
            r13 = 1
            goto L_0x01ff
        L_0x01fe:
            r13 = 0
        L_0x01ff:
            if (r13 == 0) goto L_0x0202
            goto L_0x0203
        L_0x0202:
            r8 = 0
        L_0x0203:
            if (r8 != 0) goto L_0x022f
        L_0x0205:
            if (r15 == 0) goto L_0x020c
            java.lang.String r8 = r15.attr(r4)
            goto L_0x020d
        L_0x020c:
            r8 = 0
        L_0x020d:
            if (r8 != 0) goto L_0x022f
            java.lang.String r8 = "div.before-video-player img"
            org.jsoup.nodes.Element r8 = r10.selectFirst(r8)
            if (r8 == 0) goto L_0x021c
            java.lang.String r8 = r8.attr(r4)
            goto L_0x021d
        L_0x021c:
            r8 = 0
        L_0x021d:
            if (r8 != 0) goto L_0x022f
            java.lang.String r8 = "meta[property='og:image']"
            org.jsoup.nodes.Element r8 = r10.selectFirst(r8)
            if (r8 == 0) goto L_0x022e
            java.lang.String r11 = "content"
            java.lang.String r8 = r8.attr(r11)
            goto L_0x022f
        L_0x022e:
            r8 = 0
        L_0x022f:
            java.lang.String r22 = com.lagradost.cloudstream3.MainAPIKt.fixUrlNull(r3, r8)
            java.lang.String r3 = "h1"
            org.jsoup.nodes.Element r3 = r10.selectFirst(r3)
            if (r3 == 0) goto L_0x0816
            java.lang.String r3 = r3.text()
            if (r3 != 0) goto L_0x024b
            r17 = r2
            r32 = r10
            r18 = r15
            r13 = r22
            goto L_0x081e
        L_0x024b:
            r13 = r3
            r3 = r13
            java.lang.CharSequence r3 = (java.lang.CharSequence) r3
            kotlin.text.Regex r8 = new kotlin.text.Regex
            java.lang.String r11 = "\\(.*\\)"
            r8.<init>(r11)
            java.lang.String r11 = ""
            java.lang.String r3 = r8.replace(r3, r11)
            java.lang.CharSequence r3 = (java.lang.CharSequence) r3
            java.lang.CharSequence r3 = kotlin.text.StringsKt.trim(r3)
            java.lang.String r3 = r3.toString()
            java.lang.String r8 = "h1 a"
            org.jsoup.nodes.Element r8 = r10.selectFirst(r8)
            if (r8 == 0) goto L_0x027e
            java.lang.String r8 = r8.text()
            if (r8 == 0) goto L_0x027e
            java.lang.Integer r8 = kotlin.text.StringsKt.toIntOrNull(r8)
            if (r8 != 0) goto L_0x027b
            goto L_0x027e
        L_0x027b:
            r23 = r8
            goto L_0x028f
        L_0x027e:
            java.lang.String r8 = "Yapım Yılı"
            java.lang.String r8 = load$getDataByLabel(r10, r8)
            if (r8 == 0) goto L_0x028d
            java.lang.Integer r8 = kotlin.text.StringsKt.toIntOrNull(r8)
            r23 = r8
            goto L_0x028f
        L_0x028d:
            r23 = 0
        L_0x028f:
            java.lang.String r8 = "p.summary-text"
            org.jsoup.nodes.Element r8 = r10.selectFirst(r8)
            if (r8 == 0) goto L_0x02ae
            java.lang.String r8 = r8.text()
            if (r8 == 0) goto L_0x02ae
            java.lang.CharSequence r8 = (java.lang.CharSequence) r8
            java.lang.CharSequence r8 = kotlin.text.StringsKt.trim(r8)
            java.lang.String r8 = r8.toString()
            if (r8 != 0) goto L_0x02ab
            goto L_0x02ae
        L_0x02ab:
            r24 = r8
            goto L_0x02d2
        L_0x02ae:
            java.lang.String r8 = "h6:contains(Film Özeti) + p"
            org.jsoup.nodes.Element r8 = r10.selectFirst(r8)
            if (r8 == 0) goto L_0x02c9
            java.lang.String r8 = r8.text()
            if (r8 == 0) goto L_0x02c9
            java.lang.CharSequence r8 = (java.lang.CharSequence) r8
            java.lang.CharSequence r8 = kotlin.text.StringsKt.trim(r8)
            java.lang.String r8 = r8.toString()
            r24 = r8
            goto L_0x02d2
        L_0x02c9:
            java.lang.String r8 = "Film Özeti"
            java.lang.String r8 = load$getDataByLabel(r10, r8)
            r24 = r8
        L_0x02d2:
            java.lang.String r8 = "section h1 + span + ul a, section h1 + ul li a"
            org.jsoup.select.Elements r8 = r10.select(r8)
            java.lang.Iterable r8 = (java.lang.Iterable) r8
            r11 = 0
            java.util.ArrayList r12 = new java.util.ArrayList
            r14 = 10
            int r6 = kotlin.collections.CollectionsKt.collectionSizeOrDefault(r8, r14)
            r12.<init>(r6)
            r6 = r12
            java.util.Collection r6 = (java.util.Collection) r6
            r12 = r8
            r16 = 0
            java.util.Iterator r17 = r12.iterator()
        L_0x02f1:
            boolean r18 = r17.hasNext()
            if (r18 == 0) goto L_0x0309
            java.lang.Object r18 = r17.next()
            r19 = r18
            org.jsoup.nodes.Element r19 = (org.jsoup.nodes.Element) r19
            r21 = 0
            java.lang.String r7 = r19.text()
            r6.add(r7)
            goto L_0x02f1
        L_0x0309:
            java.util.List r6 = (java.util.List) r6
            java.util.Collection r6 = (java.util.Collection) r6
            boolean r7 = r6.isEmpty()
            if (r7 == 0) goto L_0x0350
            r6 = 0
            java.lang.String r7 = "div:has(> span:contains(Tür)) > div a"
            org.jsoup.select.Elements r7 = r10.select(r7)
            java.lang.Iterable r7 = (java.lang.Iterable) r7
            r8 = 0
            java.util.ArrayList r11 = new java.util.ArrayList
            int r12 = kotlin.collections.CollectionsKt.collectionSizeOrDefault(r7, r14)
            r11.<init>(r12)
            java.util.Collection r11 = (java.util.Collection) r11
            r12 = r7
            r16 = 0
            java.util.Iterator r17 = r12.iterator()
        L_0x0331:
            boolean r18 = r17.hasNext()
            if (r18 == 0) goto L_0x034b
            java.lang.Object r18 = r17.next()
            r19 = r18
            org.jsoup.nodes.Element r19 = (org.jsoup.nodes.Element) r19
            r21 = 0
            java.lang.String r14 = r19.text()
            r11.add(r14)
            r14 = 10
            goto L_0x0331
        L_0x034b:
            java.util.List r11 = (java.util.List) r11
            r6 = r11
        L_0x0350:
            java.util.List r6 = (java.util.List) r6
            java.lang.String r7 = "IMDB Puanı"
            java.lang.String r27 = load$getDataByLabel(r10, r7)
            if (r27 == 0) goto L_0x036a
            r31 = 4
            r32 = 0
            java.lang.String r28 = ","
            java.lang.String r29 = "."
            r30 = 0
            java.lang.String r7 = kotlin.text.StringsKt.replace$default(r27, r28, r29, r30, r31, r32)
            goto L_0x036b
        L_0x036a:
            r7 = 0
        L_0x036b:
            java.lang.String r8 = "Süre"
            java.lang.String r8 = load$getDataByLabel(r10, r8)
            if (r8 == 0) goto L_0x039c
            r12 = 0
            kotlin.text.Regex r14 = new kotlin.text.Regex
            java.lang.String r11 = "\\d+"
            r14.<init>(r11)
            r11 = r8
            java.lang.CharSequence r11 = (java.lang.CharSequence) r11
            r17 = r2
            r18 = r3
            r19 = r5
            r2 = 2
            r3 = 0
            r5 = 0
            kotlin.text.MatchResult r11 = kotlin.text.Regex.find$default(r14, r11, r3, r2, r5)
            if (r11 == 0) goto L_0x0398
            java.lang.String r2 = r11.getValue()
            if (r2 == 0) goto L_0x0398
            java.lang.Integer r2 = kotlin.text.StringsKt.toIntOrNull(r2)
            goto L_0x0399
        L_0x0398:
            r2 = 0
        L_0x0399:
            r27 = r2
            goto L_0x03a4
        L_0x039c:
            r17 = r2
            r18 = r3
            r19 = r5
            r27 = 0
        L_0x03a4:
            java.lang.String r2 = "div.swiper-slide"
            org.jsoup.select.Elements r2 = r10.select(r2)
            java.lang.Iterable r2 = (java.lang.Iterable) r2
            r3 = 0
            java.util.ArrayList r5 = new java.util.ArrayList
            r8 = 10
            int r11 = kotlin.collections.CollectionsKt.collectionSizeOrDefault(r2, r8)
            r5.<init>(r11)
            java.util.Collection r5 = (java.util.Collection) r5
            r8 = r2
            r11 = 0
            java.util.Iterator r12 = r8.iterator()
        L_0x03c0:
            boolean r14 = r12.hasNext()
            if (r14 == 0) goto L_0x0451
            java.lang.Object r14 = r12.next()
            r21 = r2
            r2 = r14
            org.jsoup.nodes.Element r2 = (org.jsoup.nodes.Element) r2
            r28 = 0
            r29 = r3
            java.lang.String r3 = "h4"
            org.jsoup.select.Elements r30 = r2.select(r3)
            java.util.List r30 = (java.util.List) r30
            java.lang.Object r30 = kotlin.collections.CollectionsKt.firstOrNull(r30)
            org.jsoup.nodes.Element r30 = (org.jsoup.nodes.Element) r30
            if (r30 == 0) goto L_0x03f4
            java.lang.String r30 = r30.text()
            if (r30 == 0) goto L_0x03f4
            java.lang.CharSequence r30 = (java.lang.CharSequence) r30
            java.lang.CharSequence r30 = kotlin.text.StringsKt.trim(r30)
            java.lang.String r30 = r30.toString()
            goto L_0x03f6
        L_0x03f4:
            r30 = 0
        L_0x03f6:
            r31 = r6
            java.lang.String r6 = java.lang.String.valueOf(r30)
            org.jsoup.select.Elements r3 = r2.select(r3)
            java.util.List r3 = (java.util.List) r3
            java.lang.Object r3 = kotlin.collections.CollectionsKt.lastOrNull(r3)
            org.jsoup.nodes.Element r3 = (org.jsoup.nodes.Element) r3
            if (r3 == 0) goto L_0x041b
            java.lang.String r3 = r3.text()
            if (r3 == 0) goto L_0x041b
            java.lang.CharSequence r3 = (java.lang.CharSequence) r3
            java.lang.CharSequence r3 = kotlin.text.StringsKt.trim(r3)
            java.lang.String r3 = r3.toString()
            goto L_0x041c
        L_0x041b:
            r3 = 0
        L_0x041c:
            r30 = r7
            r7 = r0
            com.lagradost.cloudstream3.MainAPI r7 = (com.lagradost.cloudstream3.MainAPI) r7
            r32 = r8
            java.lang.String r8 = "img"
            org.jsoup.nodes.Element r8 = r2.selectFirst(r8)
            if (r8 == 0) goto L_0x0430
            java.lang.String r8 = r8.attr(r4)
            goto L_0x0431
        L_0x0430:
            r8 = 0
        L_0x0431:
            java.lang.String r7 = com.lagradost.cloudstream3.MainAPIKt.fixUrlNull(r7, r8)
            java.lang.String r7 = java.lang.String.valueOf(r7)
            com.lagradost.cloudstream3.Actor r8 = new com.lagradost.cloudstream3.Actor
            r8.<init>(r6, r7)
            kotlin.Pair r2 = kotlin.TuplesKt.to(r8, r3)
            r5.add(r2)
            r2 = r21
            r3 = r29
            r7 = r30
            r6 = r31
            r8 = r32
            goto L_0x03c0
        L_0x0451:
            r21 = r2
            r29 = r3
            r31 = r6
            r30 = r7
            r32 = r8
            r2 = r5
            java.util.List r2 = (java.util.List) r2
            java.util.Collection r2 = (java.util.Collection) r2
            boolean r3 = r2.isEmpty()
            if (r3 == 0) goto L_0x04c6
            r2 = 0
            java.lang.String r3 = "span:contains(Oyuncular) + div a"
            org.jsoup.select.Elements r3 = r10.select(r3)
            java.lang.Iterable r3 = (java.lang.Iterable) r3
            r4 = 0
            java.util.ArrayList r5 = new java.util.ArrayList
            r8 = 10
            int r6 = kotlin.collections.CollectionsKt.collectionSizeOrDefault(r3, r8)
            r5.<init>(r6)
            java.util.Collection r5 = (java.util.Collection) r5
            r6 = r3
            r7 = 0
            java.util.Iterator r8 = r6.iterator()
        L_0x0484:
            boolean r11 = r8.hasNext()
            if (r11 == 0) goto L_0x04ba
            java.lang.Object r11 = r8.next()
            r12 = r11
            org.jsoup.nodes.Element r12 = (org.jsoup.nodes.Element) r12
            r14 = 0
            java.lang.String r21 = r12.text()
            java.lang.CharSequence r21 = (java.lang.CharSequence) r21
            java.lang.CharSequence r21 = kotlin.text.StringsKt.trim(r21)
            r28 = r2
            java.lang.String r2 = r21.toString()
            r21 = r3
            com.lagradost.cloudstream3.Actor r3 = new com.lagradost.cloudstream3.Actor
            r29 = r4
            r4 = 0
            r3.<init>(r2, r4)
            kotlin.Pair r2 = kotlin.TuplesKt.to(r3, r4)
            r5.add(r2)
            r3 = r21
            r2 = r28
            r4 = r29
            goto L_0x0484
        L_0x04ba:
            r28 = r2
            r21 = r3
            r29 = r4
            r2 = r5
            java.util.List r2 = (java.util.List) r2
        L_0x04c6:
            r28 = r2
            java.util.List r28 = (java.util.List) r28
            r2 = r19
            java.lang.CharSequence r2 = (java.lang.CharSequence) r2
            java.lang.String r3 = "/anime/"
            java.lang.CharSequence r3 = (java.lang.CharSequence) r3
            r4 = 2
            r5 = 0
            r6 = 0
            boolean r2 = kotlin.text.StringsKt.contains$default(r2, r3, r5, r4, r6)
            if (r2 != 0) goto L_0x04ed
            r2 = r19
            java.lang.CharSequence r2 = (java.lang.CharSequence) r2
            java.lang.String r3 = "/animeler/"
            java.lang.CharSequence r3 = (java.lang.CharSequence) r3
            boolean r2 = kotlin.text.StringsKt.contains$default(r2, r3, r5, r4, r6)
            if (r2 == 0) goto L_0x04eb
            goto L_0x04ed
        L_0x04eb:
            r5 = 0
            goto L_0x04ee
        L_0x04ed:
            r5 = 1
        L_0x04ee:
            r2 = r5
            r3 = r19
            java.lang.CharSequence r3 = (java.lang.CharSequence) r3
            java.lang.String r4 = "/dizi/"
            java.lang.CharSequence r4 = (java.lang.CharSequence) r4
            r5 = 2
            r6 = 0
            r7 = 0
            boolean r11 = kotlin.text.StringsKt.contains$default(r3, r4, r6, r5, r7)
            if (r2 != 0) goto L_0x05b8
            if (r11 == 0) goto L_0x051c
            r21 = r13
            r4 = r18
            r5 = r19
            r13 = r22
            r14 = r27
            r12 = r30
            r22 = r2
            r19 = r11
            r18 = r15
            r15 = r23
            r11 = r24
            r2 = r31
            goto L_0x05d0
        L_0x051c:
            r3 = r0
            com.lagradost.cloudstream3.MainAPI r3 = (com.lagradost.cloudstream3.MainAPI) r3
            com.lagradost.cloudstream3.TvType r6 = com.lagradost.cloudstream3.TvType.Movie
            com.kraptor.DiziPal$load$4 r21 = new com.kraptor.DiziPal$load$4
            r29 = 0
            r26 = r30
            r25 = r31
            r21.<init>(r22, r23, r24, r25, r26, r27, r28, r29)
            r14 = r27
            r12 = r28
            r8 = r21
            kotlin.jvm.functions.Function2 r8 = (kotlin.jvm.functions.Function2) r8
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r19)
            r9.L$0 = r4
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r10)
            r9.L$1 = r4
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r15)
            r9.L$2 = r4
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r22)
            r9.L$3 = r4
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r13)
            r9.L$4 = r4
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r18)
            r9.L$5 = r4
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r23)
            r9.L$6 = r4
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r24)
            r9.L$7 = r4
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r31)
            r9.L$8 = r4
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r30)
            r9.L$9 = r4
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r14)
            r9.L$10 = r4
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r12)
            r9.L$11 = r4
            r9.I$0 = r2
            r9.Z$0 = r11
            r4 = 5
            r9.label = r4
            r7 = r19
            r21 = r13
            r4 = r18
            r5 = r19
            r13 = r22
            r12 = r30
            r22 = r2
            r19 = r11
            r18 = r15
            r15 = r23
            r11 = r24
            r2 = r31
            java.lang.Object r3 = com.lagradost.cloudstream3.MainAPIKt.newMovieLoadResponse(r3, r4, r5, r6, r7, r8, r9)
            if (r3 != r1) goto L_0x05a2
            return r1
        L_0x05a2:
            r8 = r2
            r2 = r3
            r1 = r5
            r16 = r10
            r10 = r11
            r7 = r12
            r6 = r14
            r11 = r15
            r15 = r18
            r3 = r19
            r5 = r28
            r12 = r4
            r14 = r13
            r13 = r21
            r4 = r22
        L_0x05b7:
            return r2
        L_0x05b8:
            r21 = r13
            r4 = r18
            r5 = r19
            r13 = r22
            r14 = r27
            r12 = r30
            r22 = r2
            r19 = r11
            r18 = r15
            r15 = r23
            r11 = r24
            r2 = r31
        L_0x05d0:
            java.lang.String r3 = "div#season-options-list li a:not(.font-bold)"
            org.jsoup.select.Elements r6 = r10.select(r3)
            r3 = r10
            org.jsoup.nodes.Element r3 = (org.jsoup.nodes.Element) r3
            r7 = 1
            java.util.List r3 = load$parseEpisodes(r0, r3, r7)
            r7 = r6
            java.util.Collection r7 = (java.util.Collection) r7
            boolean r7 = r7.isEmpty()
            if (r7 != 0) goto L_0x06b6
            r7 = r6
            java.lang.Iterable r7 = (java.lang.Iterable) r7
            r8 = 0
            r20 = r6
            java.util.ArrayList r6 = new java.util.ArrayList
            r23 = r8
            r8 = 10
            int r8 = kotlin.collections.CollectionsKt.collectionSizeOrDefault(r7, r8)
            r6.<init>(r8)
            java.util.Collection r6 = (java.util.Collection) r6
            r8 = r7
            r24 = 0
            java.util.Iterator r26 = r8.iterator()
        L_0x0603:
            boolean r27 = r26.hasNext()
            if (r27 == 0) goto L_0x062c
            java.lang.Object r27 = r26.next()
            r29 = r7
            r7 = r27
            org.jsoup.nodes.Element r7 = (org.jsoup.nodes.Element) r7
            r30 = 0
            r31 = r8
            com.kraptor.DiziPal$load$otherSeasonsEpisodes$1$1 r8 = new com.kraptor.DiziPal$load$otherSeasonsEpisodes$1$1
            r32 = r10
            r10 = 0
            r8.<init>(r0, r7, r10)
            kotlin.jvm.functions.Function1 r8 = (kotlin.jvm.functions.Function1) r8
            r6.add(r8)
            r7 = r29
            r8 = r31
            r10 = r32
            goto L_0x0603
        L_0x062c:
            r29 = r7
            r31 = r8
            r32 = r10
            java.util.List r6 = (java.util.List) r6
            java.util.Collection r6 = (java.util.Collection) r6
            r7 = 0
            r8 = r6
            r10 = 0
            kotlin.jvm.functions.Function1[] r10 = new kotlin.jvm.functions.Function1[r10]
            java.lang.Object[] r6 = r8.toArray(r10)
            kotlin.jvm.functions.Function1[] r6 = (kotlin.jvm.functions.Function1[]) r6
            int r7 = r6.length
            java.lang.Object[] r6 = java.util.Arrays.copyOf(r6, r7)
            kotlin.jvm.functions.Function1[] r6 = (kotlin.jvm.functions.Function1[]) r6
            r9.L$0 = r5
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r32)
            r9.L$1 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r18)
            r9.L$2 = r7
            r9.L$3 = r13
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r21)
            r9.L$4 = r7
            r9.L$5 = r4
            r9.L$6 = r15
            r9.L$7 = r11
            r9.L$8 = r2
            r9.L$9 = r12
            r9.L$10 = r14
            r7 = r28
            r9.L$11 = r7
            java.lang.Object r8 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r20)
            r9.L$12 = r8
            r9.L$13 = r3
            r8 = r22
            r9.I$0 = r8
            r10 = r19
            r9.Z$0 = r10
            r0 = 2
            r9.label = r0
            java.lang.Object r0 = com.lagradost.cloudstream3.ParCollectionsKt.runAllAsync(r6, r9)
            if (r0 != r1) goto L_0x068a
            return r1
        L_0x068a:
            r6 = r14
            r14 = r4
            r4 = r8
            r8 = r6
            r16 = r10
            r10 = r12
            r6 = r20
            r12 = r11
            r11 = r2
        L_0x0695:
            java.lang.Iterable r0 = (java.lang.Iterable) r0
            java.util.List r0 = kotlin.collections.CollectionsKt.filterNotNull(r0)
            java.lang.Iterable r0 = (java.lang.Iterable) r0
            java.util.List r0 = kotlin.collections.CollectionsKt.flatten(r0)
            r2 = r4
            r20 = r6
            r27 = r10
            r26 = r11
            r25 = r12
            r4 = r14
            r10 = r16
            r14 = r8
            r29 = r7
            r23 = r13
            r24 = r15
            r11 = r3
            goto L_0x06d2
        L_0x06b6:
            r20 = r6
            r32 = r10
            r10 = r19
            r8 = r22
            r7 = r28
            java.util.List r0 = kotlin.collections.CollectionsKt.emptyList()
            r26 = r2
            r2 = r8
            r25 = r11
            r27 = r12
            r29 = r7
            r23 = r13
            r24 = r15
            r11 = r3
        L_0x06d2:
            r3 = r11
            java.util.Collection r3 = (java.util.Collection) r3
            r6 = r0
            java.lang.Iterable r6 = (java.lang.Iterable) r6
            java.util.List r7 = kotlin.collections.CollectionsKt.plus(r3, r6)
            if (r2 == 0) goto L_0x077b
            r3 = r33
            com.lagradost.cloudstream3.MainAPI r3 = (com.lagradost.cloudstream3.MainAPI) r3
            com.lagradost.cloudstream3.TvType r6 = com.lagradost.cloudstream3.TvType.Anime
            com.kraptor.DiziPal$load$2 r22 = new com.kraptor.DiziPal$load$2
            r30 = 0
            r28 = r7
            r22.<init>(r23, r24, r25, r26, r27, r28, r29, r30)
            r12 = r28
            r8 = r22
            kotlin.jvm.functions.Function2 r8 = (kotlin.jvm.functions.Function2) r8
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r5)
            r9.L$0 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r32)
            r9.L$1 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r18)
            r9.L$2 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r23)
            r9.L$3 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r21)
            r9.L$4 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r4)
            r9.L$5 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r24)
            r9.L$6 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r25)
            r9.L$7 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r26)
            r9.L$8 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r27)
            r9.L$9 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r14)
            r9.L$10 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r29)
            r9.L$11 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r20)
            r9.L$12 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r11)
            r9.L$13 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r0)
            r9.L$14 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r12)
            r9.L$15 = r7
            r9.I$0 = r2
            r9.Z$0 = r10
            r7 = 3
            r9.label = r7
            r7 = 0
            java.lang.Object r3 = com.lagradost.cloudstream3.MainAPIKt.newAnimeLoadResponse(r3, r4, r5, r6, r7, r8, r9)
            if (r3 != r1) goto L_0x0763
            return r1
        L_0x0763:
            r15 = r4
            r16 = r10
            r6 = r11
            r4 = r12
            r10 = r14
            r7 = r20
            r1 = r21
            r14 = r24
            r13 = r25
            r12 = r26
            r11 = r27
            r8 = r29
        L_0x0777:
            com.lagradost.cloudstream3.LoadResponse r3 = (com.lagradost.cloudstream3.LoadResponse) r3
            goto L_0x0815
        L_0x077b:
            r12 = r7
            r3 = r33
            com.lagradost.cloudstream3.MainAPI r3 = (com.lagradost.cloudstream3.MainAPI) r3
            com.lagradost.cloudstream3.TvType r6 = com.lagradost.cloudstream3.TvType.TvSeries
            com.kraptor.DiziPal$load$3 r22 = new com.kraptor.DiziPal$load$3
            r30 = 0
            r28 = r14
            r22.<init>(r23, r24, r25, r26, r27, r28, r29, r30)
            r8 = r22
            kotlin.jvm.functions.Function2 r8 = (kotlin.jvm.functions.Function2) r8
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r5)
            r9.L$0 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r32)
            r9.L$1 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r18)
            r9.L$2 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r23)
            r9.L$3 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r21)
            r9.L$4 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r4)
            r9.L$5 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r24)
            r9.L$6 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r25)
            r9.L$7 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r26)
            r9.L$8 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r27)
            r9.L$9 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r28)
            r9.L$10 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r29)
            r9.L$11 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r20)
            r9.L$12 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r11)
            r9.L$13 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r0)
            r9.L$14 = r7
            java.lang.Object r7 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r12)
            r9.L$15 = r7
            r9.I$0 = r2
            r9.Z$0 = r10
            r7 = 4
            r9.label = r7
            r7 = r12
            java.lang.Object r3 = com.lagradost.cloudstream3.MainAPIKt.newTvSeriesLoadResponse(r3, r4, r5, r6, r7, r8, r9)
            if (r3 != r1) goto L_0x07fe
            return r1
        L_0x07fe:
            r15 = r4
            r4 = r7
            r16 = r10
            r6 = r11
            r7 = r20
            r1 = r21
            r14 = r24
            r13 = r25
            r12 = r26
            r11 = r27
            r10 = r28
            r8 = r29
        L_0x0813:
            com.lagradost.cloudstream3.LoadResponse r3 = (com.lagradost.cloudstream3.LoadResponse) r3
        L_0x0815:
            return r3
        L_0x0816:
            r17 = r2
            r32 = r10
            r18 = r15
            r13 = r22
        L_0x081e:
            r25 = 0
            return r25
        */
        throw new UnsupportedOperationException("Method not decompiled: com.kraptor.DiziPal.load(java.lang.String, kotlin.coroutines.Continuation):java.lang.Object");
    }

    private static final String load$getDataByLabel(Document document, String label) {
        String text;
        Element last = document.select("span:contains(" + label + ") + div").last();
        if (last == null || (text = last.text()) == null) {
            return null;
        }
        return StringsKt.trim(text).toString();
    }

    /* access modifiers changed from: private */
    public static final List<Episode> load$parseEpisodes(DiziPal this$0, Element doc, int seasonNum) {
        int $i$f$mapIndexedNotNull;
        String str;
        Iterable $this$forEachIndexed$iv$iv$iv;
        Iterable $this$mapIndexedNotNullTo$iv$iv;
        int $i$f$mapIndexedNotNullTo;
        Iterable $this$mapIndexedNotNullTo$iv$iv2;
        String str2;
        Iterable $this$mapIndexedNotNull$iv = doc.select("div.episode-item");
        int $i$f$mapIndexedNotNull2 = 0;
        Collection destination$iv$iv = new ArrayList();
        Iterable $this$mapIndexedNotNullTo$iv$iv3 = $this$mapIndexedNotNull$iv;
        int $i$f$mapIndexedNotNullTo2 = 0;
        Iterable $this$forEachIndexed$iv$iv$iv2 = $this$mapIndexedNotNullTo$iv$iv3;
        int index$iv$iv$iv = 0;
        for (Object item$iv$iv$iv : $this$forEachIndexed$iv$iv$iv2) {
            int index$iv$iv$iv2 = index$iv$iv$iv + 1;
            if (index$iv$iv$iv < 0) {
                CollectionsKt.throwIndexOverflow();
            }
            Element ep = (Element) item$iv$iv$iv;
            int index = index$iv$iv$iv;
            Iterable $this$mapIndexedNotNull$iv2 = $this$mapIndexedNotNull$iv;
            MainAPI mainAPI = (MainAPI) this$0;
            Element selectFirst = ep.selectFirst("a");
            if (selectFirst != null) {
                $i$f$mapIndexedNotNull = $i$f$mapIndexedNotNull2;
                str = selectFirst.attr("href");
            } else {
                $i$f$mapIndexedNotNull = $i$f$mapIndexedNotNull2;
                str = null;
            }
            String epHref = MainAPIKt.fixUrlNull(mainAPI, str);
            if (epHref == null) {
                $this$mapIndexedNotNullTo$iv$iv = $this$mapIndexedNotNullTo$iv$iv3;
                $this$forEachIndexed$iv$iv$iv = $this$forEachIndexed$iv$iv$iv2;
                $this$mapIndexedNotNullTo$iv$iv2 = null;
                int i = seasonNum;
                $i$f$mapIndexedNotNullTo = $i$f$mapIndexedNotNullTo2;
            } else {
                MainAPI mainAPI2 = (MainAPI) this$0;
                Element it = ep.selectFirst("img");
                if (it != null) {
                    $this$mapIndexedNotNullTo$iv$iv = $this$mapIndexedNotNullTo$iv$iv3;
                    CharSequence attr = it.attr("data-src");
                    if (attr.length() == 0) {
                        attr = it.attr("src");
                    }
                    str2 = (String) attr;
                } else {
                    $this$mapIndexedNotNullTo$iv$iv = $this$mapIndexedNotNullTo$iv$iv3;
                    str2 = null;
                }
                $i$f$mapIndexedNotNullTo = $i$f$mapIndexedNotNullTo2;
                $this$forEachIndexed$iv$iv$iv = $this$forEachIndexed$iv$iv$iv2;
                $this$mapIndexedNotNullTo$iv$iv2 = MainAPIKt.newEpisode((MainAPI) this$0, epHref, new DiziPal$$ExternalSyntheticLambda5(StringsKt.trim(ep.select("h4.text-white.text-sm").text()).toString(), seasonNum, index, MainAPIKt.fixUrlNull(mainAPI2, str2)));
            }
            if ($this$mapIndexedNotNullTo$iv$iv2 != null) {
                destination$iv$iv.add($this$mapIndexedNotNullTo$iv$iv2);
            }
            Element element = doc;
            index$iv$iv$iv = index$iv$iv$iv2;
            $this$mapIndexedNotNull$iv = $this$mapIndexedNotNull$iv2;
            $i$f$mapIndexedNotNullTo2 = $i$f$mapIndexedNotNullTo;
            $i$f$mapIndexedNotNull2 = $i$f$mapIndexedNotNull;
            $this$mapIndexedNotNullTo$iv$iv3 = $this$mapIndexedNotNullTo$iv$iv;
            $this$forEachIndexed$iv$iv$iv2 = $this$forEachIndexed$iv$iv$iv;
        }
        return (List) destination$iv$iv;
    }

    /* access modifiers changed from: private */
    public static final Unit load$parseEpisodes$lambda$6$1(String $epTitle, int $seasonNum, int $index, String $epPoster, Episode $this$newEpisode) {
        $this$newEpisode.setName($epTitle);
        $this$newEpisode.setSeason(Integer.valueOf($seasonNum));
        $this$newEpisode.setEpisode(Integer.valueOf($index + 1));
        $this$newEpisode.setPosterUrl($epPoster);
        return Unit.INSTANCE;
    }

    /* Debug info: failed to restart local var, previous not found, register: 39 */
    /*  JADX ERROR: NullPointerException in pass: CodeShrinkVisitor
        java.lang.NullPointerException
        */
    @org.jetbrains.annotations.Nullable
    public java.lang.Object loadLinks(@org.jetbrains.annotations.NotNull java.lang.String r40, boolean r41, @org.jetbrains.annotations.NotNull kotlin.jvm.functions.Function1<? super com.lagradost.cloudstream3.SubtitleFile, kotlin.Unit> r42, @org.jetbrains.annotations.NotNull kotlin.jvm.functions.Function1<? super com.lagradost.cloudstream3.utils.ExtractorLink, kotlin.Unit> r43, @org.jetbrains.annotations.NotNull kotlin.coroutines.Continuation<? super java.lang.Boolean> r44) {
        /*
            r39 = this;
            r0 = r39
            r1 = r44
            boolean r2 = r1 instanceof com.kraptor.DiziPal$loadLinks$1
            if (r2 == 0) goto L_0x0018
            r2 = r1
            com.kraptor.DiziPal$loadLinks$1 r2 = (com.kraptor.DiziPal$loadLinks$1) r2
            int r3 = r2.label
            r4 = -2147483648(0xffffffff80000000, float:-0.0)
            r3 = r3 & r4
            if (r3 == 0) goto L_0x0018
            int r3 = r2.label
            int r3 = r3 - r4
            r2.label = r3
            goto L_0x001d
        L_0x0018:
            com.kraptor.DiziPal$loadLinks$1 r2 = new com.kraptor.DiziPal$loadLinks$1
            r2.<init>(r0, r1)
        L_0x001d:
            r6 = r2
            java.lang.Object r2 = r6.result
            java.lang.Object r3 = kotlin.coroutines.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED()
            int r4 = r6.label
            java.lang.String r8 = "]"
            java.lang.String r9 = "["
            r10 = 47
            java.lang.String r20 = "Turkish"
            r11 = 0
            switch(r4) {
                case 0: goto L_0x01a9;
                case 1: goto L_0x0186;
                case 2: goto L_0x015d;
                case 3: goto L_0x0139;
                case 4: goto L_0x00b7;
                case 5: goto L_0x0068;
                case 6: goto L_0x003a;
                default: goto L_0x0032;
            }
        L_0x0032:
            java.lang.IllegalStateException r0 = new java.lang.IllegalStateException
            java.lang.String r1 = "call to 'resume' before 'invoke' with coroutine"
            r0.<init>(r1)
            throw r0
        L_0x003a:
            boolean r3 = r6.Z$0
            java.lang.Object r4 = r6.L$8
            kotlin.jvm.functions.Function1 r4 = (kotlin.jvm.functions.Function1) r4
            java.lang.Object r5 = r6.L$7
            java.lang.String r5 = (java.lang.String) r5
            java.lang.Object r7 = r6.L$6
            java.lang.String r7 = (java.lang.String) r7
            java.lang.Object r8 = r6.L$5
            java.lang.String r8 = (java.lang.String) r8
            java.lang.Object r9 = r6.L$4
            java.lang.String r9 = (java.lang.String) r9
            java.lang.Object r10 = r6.L$3
            org.jsoup.nodes.Document r10 = (org.jsoup.nodes.Document) r10
            java.lang.Object r11 = r6.L$2
            kotlin.jvm.functions.Function1 r11 = (kotlin.jvm.functions.Function1) r11
            java.lang.Object r12 = r6.L$1
            kotlin.jvm.functions.Function1 r12 = (kotlin.jvm.functions.Function1) r12
            java.lang.Object r14 = r6.L$0
            java.lang.String r14 = (java.lang.String) r14
            kotlin.ResultKt.throwOnFailure(r2)
            r13 = r12
            r12 = r0
            r0 = r2
            goto L_0x0693
        L_0x0068:
            boolean r4 = r6.Z$0
            java.lang.Object r5 = r6.L$12
            kotlin.jvm.functions.Function1 r5 = (kotlin.jvm.functions.Function1) r5
            java.lang.Object r7 = r6.L$11
            java.lang.String r7 = (java.lang.String) r7
            java.lang.Object r8 = r6.L$10
            java.util.List r8 = (java.util.List) r8
            java.lang.Object r9 = r6.L$9
            java.lang.String r9 = (java.lang.String) r9
            java.lang.Object r10 = r6.L$8
            java.lang.String r10 = (java.lang.String) r10
            java.lang.Object r11 = r6.L$7
            java.lang.String r11 = (java.lang.String) r11
            java.lang.Object r12 = r6.L$6
            java.lang.String r12 = (java.lang.String) r12
            java.lang.Object r15 = r6.L$5
            java.lang.String r15 = (java.lang.String) r15
            java.lang.Object r14 = r6.L$4
            java.lang.String r14 = (java.lang.String) r14
            java.lang.Object r13 = r6.L$3
            org.jsoup.nodes.Document r13 = (org.jsoup.nodes.Document) r13
            java.lang.Object r0 = r6.L$2
            kotlin.jvm.functions.Function1 r0 = (kotlin.jvm.functions.Function1) r0
            r41 = r0
            java.lang.Object r0 = r6.L$1
            kotlin.jvm.functions.Function1 r0 = (kotlin.jvm.functions.Function1) r0
            r42 = r0
            java.lang.Object r0 = r6.L$0
            java.lang.String r0 = (java.lang.String) r0
            kotlin.ResultKt.throwOnFailure(r2)
            r16 = r2
            r1 = r10
            r24 = r13
            r13 = r0
            r0 = r3
            r10 = r4
            r2 = r9
            r3 = r12
            r9 = r41
            r12 = r42
            r4 = r16
            goto L_0x05fe
        L_0x00b7:
            int r0 = r6.I$1
            int r4 = r6.I$0
            boolean r10 = r6.Z$0
            java.lang.Object r11 = r6.L$15
            kotlin.jvm.functions.Function1 r11 = (kotlin.jvm.functions.Function1) r11
            java.lang.Object r13 = r6.L$14
            java.lang.String r13 = (java.lang.String) r13
            java.lang.Object r14 = r6.L$13
            java.lang.String r14 = (java.lang.String) r14
            java.lang.Object r15 = r6.L$12
            java.lang.String r15 = (java.lang.String) r15
            java.lang.Object r5 = r6.L$11
            java.lang.String r5 = (java.lang.String) r5
            java.lang.Object r7 = r6.L$10
            java.lang.Object r12 = r6.L$9
            java.util.Iterator r12 = (java.util.Iterator) r12
            r22 = r0
            java.lang.Object r0 = r6.L$8
            java.lang.Iterable r0 = (java.lang.Iterable) r0
            r41 = r0
            java.lang.Object r0 = r6.L$7
            java.lang.String r0 = (java.lang.String) r0
            r23 = r0
            java.lang.Object r0 = r6.L$6
            java.lang.String r0 = (java.lang.String) r0
            r24 = r0
            java.lang.Object r0 = r6.L$5
            java.lang.String r0 = (java.lang.String) r0
            r25 = r0
            java.lang.Object r0 = r6.L$4
            java.lang.String r0 = (java.lang.String) r0
            r26 = r0
            java.lang.Object r0 = r6.L$3
            org.jsoup.nodes.Document r0 = (org.jsoup.nodes.Document) r0
            r27 = r0
            java.lang.Object r0 = r6.L$2
            kotlin.jvm.functions.Function1 r0 = (kotlin.jvm.functions.Function1) r0
            r43 = r0
            java.lang.Object r0 = r6.L$1
            kotlin.jvm.functions.Function1 r0 = (kotlin.jvm.functions.Function1) r0
            r42 = r0
            java.lang.Object r0 = r6.L$0
            java.lang.String r0 = (java.lang.String) r0
            kotlin.ResultKt.throwOnFailure(r2)
            r16 = r15
            r15 = r0
            r0 = r22
            r22 = r25
            r25 = r16
            r29 = r2
            r36 = r6
            r18 = r7
            r33 = r13
            r16 = r14
            r30 = r27
            r13 = r42
            r7 = r29
            r6 = r3
            r2 = r9
            r14 = r12
            r9 = r39
            r3 = r1
            r1 = r8
            r12 = r11
            r8 = r4
            r11 = r10
            r10 = r43
            r4 = r41
            goto L_0x04ba
        L_0x0139:
            boolean r0 = r6.Z$0
            java.lang.Object r3 = r6.L$6
            java.lang.String r3 = (java.lang.String) r3
            java.lang.Object r4 = r6.L$5
            java.lang.String r4 = (java.lang.String) r4
            java.lang.Object r5 = r6.L$4
            java.lang.String r5 = (java.lang.String) r5
            java.lang.Object r7 = r6.L$3
            org.jsoup.nodes.Document r7 = (org.jsoup.nodes.Document) r7
            java.lang.Object r8 = r6.L$2
            kotlin.jvm.functions.Function1 r8 = (kotlin.jvm.functions.Function1) r8
            java.lang.Object r9 = r6.L$1
            kotlin.jvm.functions.Function1 r9 = (kotlin.jvm.functions.Function1) r9
            java.lang.Object r10 = r6.L$0
            java.lang.String r10 = (java.lang.String) r10
            kotlin.ResultKt.throwOnFailure(r2)
            r1 = r2
            goto L_0x0333
        L_0x015d:
            boolean r0 = r6.Z$0
            java.lang.Object r4 = r6.L$4
            java.lang.String r4 = (java.lang.String) r4
            java.lang.Object r5 = r6.L$3
            org.jsoup.nodes.Document r5 = (org.jsoup.nodes.Document) r5
            java.lang.Object r7 = r6.L$2
            kotlin.jvm.functions.Function1 r7 = (kotlin.jvm.functions.Function1) r7
            java.lang.Object r12 = r6.L$1
            kotlin.jvm.functions.Function1 r12 = (kotlin.jvm.functions.Function1) r12
            java.lang.Object r13 = r6.L$0
            java.lang.String r13 = (java.lang.String) r13
            kotlin.ResultKt.throwOnFailure(r2)
            r10 = r0
            r0 = r3
            r24 = r5
            r31 = r8
            r32 = r9
            r1 = 2
            r23 = 0
            r3 = r2
            r9 = r7
            r14 = r4
            goto L_0x02b4
        L_0x0186:
            boolean r0 = r6.Z$0
            java.lang.Object r4 = r6.L$2
            kotlin.jvm.functions.Function1 r4 = (kotlin.jvm.functions.Function1) r4
            java.lang.Object r5 = r6.L$1
            kotlin.jvm.functions.Function1 r5 = (kotlin.jvm.functions.Function1) r5
            java.lang.Object r7 = r6.L$0
            java.lang.String r7 = (java.lang.String) r7
            kotlin.ResultKt.throwOnFailure(r2)
            r1 = r3
            r3 = r0
            r0 = r1
            r1 = r5
            r5 = r4
            r4 = r1
            r21 = r7
            r31 = r8
            r32 = r9
            r1 = 47
            r23 = 0
            r7 = r2
            goto L_0x020b
        L_0x01a9:
            kotlin.ResultKt.throwOnFailure(r2)
            r0 = r3
            com.lagradost.nicehttp.Requests r3 = com.lagradost.cloudstream3.MainActivityKt.getApp()
            com.kraptor.DiziPal$CloudflareInterceptor r4 = r39.getInterceptor()
            r14 = r4
            okhttp3.Interceptor r14 = (okhttp3.Interceptor) r14
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r40)
            r6.L$0 = r4
            r4 = r42
            r6.L$1 = r4
            r5 = r43
            r6.L$2 = r5
            r7 = r41
            r6.Z$0 = r7
            r12 = 1
            r6.label = r12
            r5 = 0
            r17 = r6
            r6 = 0
            r7 = 0
            r13 = r8
            r8 = 0
            r15 = r9
            r9 = 0
            r22 = 47
            r10 = 0
            r23 = 0
            r11 = 0
            r24 = r13
            r25 = 1
            r12 = 0
            r26 = r15
            r15 = 0
            r27 = 0
            r16 = 0
            r28 = 93
            r18 = 3582(0xdfe, float:5.02E-42)
            r29 = 91
            r19 = 0
            r4 = r40
            r31 = r24
            r32 = r26
            r1 = 47
            java.lang.Object r3 = com.lagradost.nicehttp.Requests.get$default(r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r14, r15, r16, r17, r18, r19)
            r6 = r17
            if (r3 != r0) goto L_0x0202
            return r0
        L_0x0202:
            r21 = r40
            r4 = r42
            r5 = r43
            r7 = r3
            r3 = r41
        L_0x020b:
            com.lagradost.nicehttp.NiceResponse r7 = (com.lagradost.nicehttp.NiceResponse) r7
            org.jsoup.nodes.Document r7 = r7.getDocument()
            java.lang.String r8 = ".responsive-player iframe"
            org.jsoup.nodes.Element r8 = r7.selectFirst(r8)
            java.lang.String r9 = "src"
            if (r8 == 0) goto L_0x0222
            java.lang.String r8 = r8.attr(r9)
            if (r8 != 0) goto L_0x0248
        L_0x0222:
            java.lang.String r8 = ".series-player-container iframe"
            org.jsoup.nodes.Element r8 = r7.selectFirst(r8)
            if (r8 == 0) goto L_0x022f
            java.lang.String r14 = r8.attr(r9)
            goto L_0x0230
        L_0x022f:
            r14 = 0
        L_0x0230:
            if (r14 != 0) goto L_0x0247
            java.lang.String r8 = "div#vast_new iframe"
            org.jsoup.nodes.Element r8 = r7.selectFirst(r8)
            if (r8 == 0) goto L_0x023f
            java.lang.String r14 = r8.attr(r9)
            goto L_0x0240
        L_0x023f:
            r14 = 0
        L_0x0240:
            if (r14 != 0) goto L_0x0247
            java.lang.Boolean r0 = kotlin.coroutines.jvm.internal.Boxing.boxBoolean(r23)
            return r0
        L_0x0247:
            r8 = r14
        L_0x0248:
            com.lagradost.nicehttp.Requests r9 = com.lagradost.cloudstream3.MainActivityKt.getApp()
            java.lang.StringBuilder r10 = new java.lang.StringBuilder
            r10.<init>()
            java.lang.String r11 = r39.getMainUrl()
            java.lang.StringBuilder r10 = r10.append(r11)
            java.lang.StringBuilder r10 = r10.append(r1)
            java.lang.String r10 = r10.toString()
            java.lang.Object r11 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r21)
            r6.L$0 = r11
            r6.L$1 = r4
            r6.L$2 = r5
            java.lang.Object r11 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r7)
            r6.L$3 = r11
            r6.L$4 = r8
            r6.Z$0 = r3
            r11 = 2
            r6.label = r11
            r12 = r5
            r5 = 0
            r13 = r7
            r7 = 0
            r14 = r4
            r4 = r8
            r8 = 0
            r15 = r3
            r3 = r9
            r9 = 0
            r17 = r6
            r6 = r10
            r10 = 0
            r33 = 2
            r11 = 0
            r18 = r12
            r16 = r13
            r12 = 0
            r19 = r14
            r14 = 0
            r22 = r15
            r15 = 0
            r24 = r16
            r16 = 0
            r25 = r18
            r18 = 4090(0xffa, float:5.731E-42)
            r26 = r19
            r19 = 0
            r1 = 2
            java.lang.Object r3 = com.lagradost.nicehttp.Requests.get$default(r3, r4, r5, r6, r7, r8, r9, r10, r11, r12, r14, r15, r16, r17, r18, r19)
            r6 = r17
            if (r3 != r0) goto L_0x02ab
            return r0
        L_0x02ab:
            r13 = r21
            r10 = r22
            r9 = r25
            r12 = r26
            r14 = r4
        L_0x02b4:
            com.lagradost.nicehttp.NiceResponse r3 = (com.lagradost.nicehttp.NiceResponse) r3
            java.lang.String r15 = r3.getText()
            kotlin.text.Regex r3 = new kotlin.text.Regex
            java.lang.String r4 = "file:\"([^\"]+)"
            r3.<init>(r4)
            r4 = r15
            java.lang.CharSequence r4 = (java.lang.CharSequence) r4
            r5 = 0
            r7 = 0
            kotlin.text.MatchResult r3 = kotlin.text.Regex.find$default(r3, r4, r5, r1, r7)
            if (r3 == 0) goto L_0x02da
            java.util.List r3 = r3.getGroupValues()
            if (r3 == 0) goto L_0x02da
            r11 = 1
            java.lang.Object r3 = r3.get(r11)
            java.lang.String r3 = (java.lang.String) r3
            goto L_0x02dc
        L_0x02da:
            r11 = 1
            r3 = 0
        L_0x02dc:
            if (r3 != 0) goto L_0x0334
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r1.<init>()
            java.lang.String r4 = r39.getMainUrl()
            java.lang.StringBuilder r1 = r1.append(r4)
            r4 = 47
            java.lang.StringBuilder r1 = r1.append(r4)
            java.lang.String r1 = r1.toString()
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r13)
            r6.L$0 = r4
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r12)
            r6.L$1 = r4
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r9)
            r6.L$2 = r4
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r24)
            r6.L$3 = r4
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r14)
            r6.L$4 = r4
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r15)
            r6.L$5 = r4
            java.lang.Object r4 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r3)
            r6.L$6 = r4
            r6.Z$0 = r10
            r4 = 3
            r6.label = r4
            java.lang.Object r1 = com.lagradost.cloudstream3.utils.ExtractorApiKt.loadExtractor(r14, r1, r12, r9, r6)
            if (r1 != r0) goto L_0x032b
            return r0
        L_0x032b:
            r8 = r9
            r0 = r10
            r9 = r12
            r10 = r13
            r5 = r14
            r4 = r15
            r7 = r24
        L_0x0333:
            return r1
        L_0x0334:
            kotlin.text.Regex r4 = new kotlin.text.Regex
            java.lang.String r7 = "\"subtitle\":\"([^\"]+)"
            r4.<init>(r7)
            r7 = r15
            java.lang.CharSequence r7 = (java.lang.CharSequence) r7
            r8 = 0
            kotlin.text.MatchResult r4 = kotlin.text.Regex.find$default(r4, r7, r5, r1, r8)
            if (r4 == 0) goto L_0x0352
            java.util.List r4 = r4.getGroupValues()
            if (r4 == 0) goto L_0x0352
            java.lang.Object r4 = r4.get(r11)
            java.lang.String r4 = (java.lang.String) r4
            goto L_0x0353
        L_0x0352:
            r4 = 0
        L_0x0353:
            if (r4 == 0) goto L_0x060e
            r7 = r4
            java.lang.CharSequence r7 = (java.lang.CharSequence) r7
            java.lang.String r8 = ","
            r11 = r8
            java.lang.CharSequence r11 = (java.lang.CharSequence) r11
            r16 = r2
            r2 = 0
            boolean r7 = kotlin.text.StringsKt.contains$default(r7, r11, r5, r1, r2)
            if (r7 == 0) goto L_0x04fa
            r33 = r4
            java.lang.CharSequence r33 = (java.lang.CharSequence) r33
            java.lang.String[] r34 = new java.lang.String[]{r8}
            r37 = 6
            r38 = 0
            r35 = 0
            r36 = 0
            java.util.List r2 = kotlin.text.StringsKt.split$default(r33, r34, r35, r36, r37, r38)
            java.lang.Iterable r2 = (java.lang.Iterable) r2
            r5 = 0
            java.util.Iterator r7 = r2.iterator()
            r8 = r39
            r11 = r12
            r12 = r7
            r7 = r6
            r6 = r5
            r5 = r0
            r0 = r44
        L_0x038a:
            boolean r18 = r12.hasNext()
            if (r18 == 0) goto L_0x04dd
            java.lang.Object r18 = r12.next()
            r1 = r18
            java.lang.String r1 = (java.lang.String) r1
            r40 = r0
            r0 = 0
            r41 = r2
            r42 = r4
            r43 = r8
            r44 = r13
            r2 = r32
            r4 = 2
            r8 = 0
            java.lang.String r13 = kotlin.text.StringsKt.substringAfter$default(r1, r2, r8, r4, r8)
            r33 = r1
            r1 = r31
            java.lang.String r13 = kotlin.text.StringsKt.substringBefore$default(r13, r1, r8, r4, r8)
            java.lang.StringBuilder r4 = new java.lang.StringBuilder
            r4.<init>()
            r8 = 91
            java.lang.StringBuilder r4 = r4.append(r8)
            java.lang.StringBuilder r4 = r4.append(r13)
            r8 = 93
            java.lang.StringBuilder r4 = r4.append(r8)
            java.lang.String r34 = r4.toString()
            r37 = 4
            r38 = 0
            java.lang.String r35 = ""
            r36 = 0
            java.lang.String r4 = kotlin.text.StringsKt.replace$default(r33, r34, r35, r36, r37, r38)
            r22 = r33
            r8 = r13
            java.lang.CharSequence r8 = (java.lang.CharSequence) r8
            java.lang.String r23 = "Turkce"
            r25 = r13
            r13 = r23
            java.lang.CharSequence r13 = (java.lang.CharSequence) r13
            r23 = r14
            r14 = 1
            boolean r8 = kotlin.text.StringsKt.contains(r8, r13, r14)
            if (r8 == 0) goto L_0x03f1
            r33 = r20
            goto L_0x042c
        L_0x03f1:
            r8 = r25
            java.lang.CharSequence r8 = (java.lang.CharSequence) r8
            java.lang.String r13 = "Ingilizce"
            java.lang.CharSequence r13 = (java.lang.CharSequence) r13
            boolean r8 = kotlin.text.StringsKt.contains(r8, r13, r14)
            java.lang.String r13 = "English"
            if (r8 == 0) goto L_0x0404
            r33 = r13
            goto L_0x042c
        L_0x0404:
            r8 = r25
            java.lang.CharSequence r8 = (java.lang.CharSequence) r8
            java.lang.String r17 = "Türkçe"
            r26 = r13
            r13 = r17
            java.lang.CharSequence r13 = (java.lang.CharSequence) r13
            boolean r8 = kotlin.text.StringsKt.contains(r8, r13, r14)
            if (r8 == 0) goto L_0x0419
            r33 = r20
            goto L_0x042c
        L_0x0419:
            r8 = r25
            java.lang.CharSequence r8 = (java.lang.CharSequence) r8
            java.lang.String r13 = "İngilizce"
            java.lang.CharSequence r13 = (java.lang.CharSequence) r13
            boolean r8 = kotlin.text.StringsKt.contains(r8, r13, r14)
            if (r8 == 0) goto L_0x042a
            r33 = r26
            goto L_0x042c
        L_0x042a:
            r33 = r25
        L_0x042c:
            r8 = r43
            com.lagradost.cloudstream3.MainAPI r8 = (com.lagradost.cloudstream3.MainAPI) r8
            java.lang.String r34 = com.lagradost.cloudstream3.MainAPIKt.fixUrl(r8, r4)
            java.lang.Object r8 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r44)
            r7.L$0 = r8
            r7.L$1 = r11
            r7.L$2 = r9
            java.lang.Object r8 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r24)
            r7.L$3 = r8
            java.lang.Object r8 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r23)
            r7.L$4 = r8
            java.lang.Object r8 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r15)
            r7.L$5 = r8
            r7.L$6 = r3
            java.lang.Object r8 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r42)
            r7.L$7 = r8
            java.lang.Object r8 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r41)
            r7.L$8 = r8
            r7.L$9 = r12
            java.lang.Object r8 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r18)
            r7.L$10 = r8
            java.lang.Object r8 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r22)
            r7.L$11 = r8
            java.lang.Object r8 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r25)
            r7.L$12 = r8
            java.lang.Object r8 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r4)
            r7.L$13 = r8
            java.lang.Object r8 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r33)
            r7.L$14 = r8
            r7.L$15 = r11
            r7.Z$0 = r10
            r7.I$0 = r6
            r7.I$1 = r0
            r8 = 4
            r7.label = r8
            r35 = 0
            r37 = 4
            r38 = 0
            r36 = r7
            java.lang.Object r7 = com.lagradost.cloudstream3.MainAPIKt.newSubtitleFile$default(r33, r34, r35, r36, r37, r38)
            if (r7 != r5) goto L_0x049b
            return r5
        L_0x049b:
            r8 = r6
            r13 = r11
            r14 = r12
            r29 = r16
            r26 = r23
            r30 = r24
            r23 = r42
            r24 = r3
            r16 = r4
            r6 = r5
            r11 = r10
            r12 = r13
            r5 = r22
            r3 = r40
            r10 = r9
            r22 = r15
            r9 = r43
            r15 = r44
            r4 = r41
        L_0x04ba:
            r12.invoke(r7)
            r31 = r1
            r32 = r2
            r0 = r3
            r2 = r4
            r5 = r6
            r6 = r8
            r8 = r9
            r9 = r10
            r10 = r11
            r11 = r13
            r12 = r14
            r13 = r15
            r15 = r22
            r4 = r23
            r3 = r24
            r14 = r26
            r16 = r29
            r24 = r30
            r7 = r36
            r1 = 2
            goto L_0x038a
        L_0x04dd:
            r40 = r0
            r41 = r2
            r42 = r4
            r36 = r7
            r43 = r8
            r44 = r13
            r23 = r14
            r12 = r43
            r1 = r5
            r13 = r11
            r2 = r16
            r8 = r36
            r11 = r42
            r16 = r44
            r5 = r3
            goto L_0x061f
        L_0x04fa:
            r1 = r31
            r2 = r32
            r8 = 0
            r11 = 2
            java.lang.String r2 = kotlin.text.StringsKt.substringAfter$default(r4, r2, r8, r11, r8)
            java.lang.String r1 = kotlin.text.StringsKt.substringBefore$default(r2, r1, r8, r11, r8)
            java.lang.StringBuilder r2 = new java.lang.StringBuilder
            r2.<init>()
            r8 = 91
            java.lang.StringBuilder r2 = r2.append(r8)
            java.lang.StringBuilder r2 = r2.append(r1)
            r8 = 93
            java.lang.StringBuilder r2 = r2.append(r8)
            java.lang.String r34 = r2.toString()
            r37 = 4
            r38 = 0
            java.lang.String r35 = ""
            r36 = 0
            r33 = r4
            java.lang.String r2 = kotlin.text.StringsKt.replace$default(r33, r34, r35, r36, r37, r38)
            java.lang.String r4 = "turkce"
            java.lang.String r7 = "tür"
            java.lang.String r8 = "tur"
            java.lang.String r11 = "tr"
            java.lang.String r5 = "türkçe"
            java.lang.String[] r4 = new java.lang.String[]{r8, r11, r5, r4, r7}
            java.util.List r11 = kotlin.collections.CollectionsKt.listOf(r4)
            r4 = r11
            java.lang.Iterable r4 = (java.lang.Iterable) r4
            r5 = 0
            boolean r7 = r4 instanceof java.util.Collection
            if (r7 == 0) goto L_0x0557
            r7 = r4
            java.util.Collection r7 = (java.util.Collection) r7
            boolean r7 = r7.isEmpty()
            if (r7 == 0) goto L_0x0557
            r40 = r1
            r23 = 0
            goto L_0x0592
        L_0x0557:
            java.util.Iterator r7 = r4.iterator()
        L_0x055b:
            boolean r8 = r7.hasNext()
            if (r8 == 0) goto L_0x058a
            java.lang.Object r8 = r7.next()
            r18 = r8
            java.lang.String r18 = (java.lang.String) r18
            r19 = 0
            r40 = r1
            r1 = r40
            java.lang.CharSequence r1 = (java.lang.CharSequence) r1
            r41 = r4
            r4 = r18
            java.lang.CharSequence r4 = (java.lang.CharSequence) r4
            r42 = r5
            r5 = 1
            boolean r1 = kotlin.text.StringsKt.contains(r1, r4, r5)
            if (r1 == 0) goto L_0x0583
            r23 = 1
            goto L_0x0592
        L_0x0583:
            r1 = r40
            r4 = r41
            r5 = r42
            goto L_0x055b
        L_0x058a:
            r40 = r1
            r41 = r4
            r42 = r5
            r23 = 0
        L_0x0592:
            if (r23 == 0) goto L_0x0595
            goto L_0x0597
        L_0x0595:
            r20 = r40
        L_0x0597:
            r1 = r39
            com.lagradost.cloudstream3.MainAPI r1 = (com.lagradost.cloudstream3.MainAPI) r1
            java.lang.String r4 = com.lagradost.cloudstream3.MainAPIKt.fixUrl(r1, r2)
            java.lang.Object r1 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r13)
            r6.L$0 = r1
            java.lang.Object r1 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r12)
            r6.L$1 = r1
            r6.L$2 = r9
            java.lang.Object r1 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r24)
            r6.L$3 = r1
            java.lang.Object r1 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r14)
            r6.L$4 = r1
            java.lang.Object r1 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r15)
            r6.L$5 = r1
            r6.L$6 = r3
            java.lang.Object r1 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r33)
            r6.L$7 = r1
            java.lang.Object r1 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r40)
            r6.L$8 = r1
            java.lang.Object r1 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r2)
            r6.L$9 = r1
            java.lang.Object r1 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r11)
            r6.L$10 = r1
            java.lang.Object r1 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r20)
            r6.L$11 = r1
            r6.L$12 = r12
            r6.Z$0 = r10
            r1 = 5
            r6.label = r1
            r5 = 0
            r7 = 4
            r8 = 0
            r1 = r3
            r3 = r20
            java.lang.Object r4 = com.lagradost.cloudstream3.MainAPIKt.newSubtitleFile$default(r3, r4, r5, r6, r7, r8)
            if (r4 != r0) goto L_0x05f6
            return r0
        L_0x05f6:
            r7 = r3
            r8 = r11
            r5 = r12
            r11 = r33
            r3 = r1
            r1 = r40
        L_0x05fe:
            r5.invoke(r4)
            r1 = r0
            r5 = r3
            r8 = r6
            r2 = r16
            r0 = r44
            r16 = r13
            r13 = r12
            r12 = r39
            goto L_0x061f
        L_0x060e:
            r16 = r2
            r1 = r3
            r33 = r4
            r5 = r1
            r8 = r6
            r11 = r33
            r1 = r0
            r16 = r13
            r0 = r44
            r13 = r12
            r12 = r39
        L_0x061f:
            java.lang.String r3 = r12.getName()
            java.lang.String r4 = r12.getName()
            com.lagradost.cloudstream3.utils.ExtractorLinkType r6 = com.lagradost.cloudstream3.utils.ExtractorLinkType.M3U8
            com.kraptor.DiziPal$loadLinks$3 r7 = new com.kraptor.DiziPal$loadLinks$3
            r40 = r0
            r0 = 0
            r7.<init>(r12, r0)
            kotlin.jvm.functions.Function2 r7 = (kotlin.jvm.functions.Function2) r7
            java.lang.Object r0 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r16)
            r8.L$0 = r0
            java.lang.Object r0 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r13)
            r8.L$1 = r0
            java.lang.Object r0 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r9)
            r8.L$2 = r0
            java.lang.Object r0 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r24)
            r8.L$3 = r0
            java.lang.Object r0 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r14)
            r8.L$4 = r0
            java.lang.Object r0 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r15)
            r8.L$5 = r0
            java.lang.Object r0 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r5)
            r8.L$6 = r0
            java.lang.Object r0 = kotlin.coroutines.jvm.internal.SpillingKt.nullOutSpilledVariable(r11)
            r8.L$7 = r0
            r8.L$8 = r9
            r0 = 0
            r8.L$9 = r0
            r8.L$10 = r0
            r8.L$11 = r0
            r8.L$12 = r0
            r8.L$13 = r0
            r8.L$14 = r0
            r8.L$15 = r0
            r8.Z$0 = r10
            r0 = 6
            r8.label = r0
            java.lang.Object r0 = com.lagradost.cloudstream3.utils.ExtractorApiKt.newExtractorLink(r3, r4, r5, r6, r7, r8)
            if (r0 != r1) goto L_0x0682
            return r1
        L_0x0682:
            r1 = r2
            r2 = r0
            r0 = r1
            r1 = r40
            r7 = r5
            r6 = r8
            r4 = r9
            r3 = r10
            r5 = r11
            r8 = r15
            r10 = r24
            r11 = r4
            r9 = r14
            r14 = r16
        L_0x0693:
            r4.invoke(r2)
            r17 = 1
            java.lang.Boolean r2 = kotlin.coroutines.jvm.internal.Boxing.boxBoolean(r17)
            return r2
        */
        throw new UnsupportedOperationException("Method not decompiled: com.kraptor.DiziPal.loadLinks(java.lang.String, boolean, kotlin.jvm.functions.Function1, kotlin.jvm.functions.Function1, kotlin.coroutines.Continuation):java.lang.Object");
    }
}
    
