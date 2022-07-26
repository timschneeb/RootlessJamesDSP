#include <stdlib.h>
#include "fdesign.h"
#ifndef M_PI
#define M_PI 3.14159265358979323846
#endif
size_t getMemSizeWarpedPFB(unsigned int N, unsigned int m)
{
	unsigned int L = 2 * m * N;
	return sizeof(WarpedPFB) + (N * sizeof(unsigned int)) + ((N + 1) * sizeof(float)) + (N * sizeof(float)) + (L * sizeof(float)) +
		(L * sizeof(float)) + (N * 2 * N * sizeof(float)) + (N * sizeof(unsigned int)) + (N * sizeof(float)) +
		(L * sizeof(float)) + (L * sizeof(float)) + (2 * m * N * sizeof(float));
}
float add_denormal_prevention_white_noise(unsigned int *rand_state)
{
	*rand_state = *rand_state * 1234567UL + 890123UL;
	int mantissa = *rand_state & 0x007F0000; // Keep only most significant bits
	int flt_rnd = mantissa | 0x1E000000; // Set exponent
	return *((float*)(&flt_rnd));
}
#include <math.h>
float* allpass_char(double alpha, unsigned int L, unsigned int *CFiltLen);
extern void cos_fib_paraunitary1(unsigned int N, unsigned int m, unsigned int L, double df, double *h_opt);
extern void subsamplingCal(unsigned int M, double alpha, double *f_def, unsigned int *Sk);
double getOptimalDF(unsigned int N, unsigned int m)
{
	if (m < 1)
		return 0.0;
	else if (m >= 1 && m < 7)
	{
		if (N < 97)
		{
			const double dfLUT[6 * 96] = { 1,1,1,1,1,1,0.286535730580963,0.313405026255228,0.302302065670699,0.304931433419360,0.304207267806462,0.295200983222171,0.280404534944377,0.261993361638258,0.188621251756131,0.243277852771489,0.215230217928873,0.200555709104807,0.222144343862097,0.183881439834043,0.126486068103919,0.152837046255515,0.146553911685564,0.143949498674247,0.201765257153999,0.156756588011550,0.0966713345320333,0.126838214621722,0.119759709102859,0.118108415531563,0.182835849707188,0.136094257302092,0.0801057846520175,0.103135778085260,0.0987321746164191,0.0961898377760306,0.198584536763086,0.143730288458311,0.0664170631394259,0.104216312645053,0.0915523406788585,0.0843631151227219,0.167637759878204,0.119383345099339,0.0583727711411420,0.0929865590348050,0.0940219341422456,0.0756257255323145,0.148852790219506,0.100002070784993,0.0508146136068421,0.0750377176002302,0.0702369292348170,0.0675945851189043,0.138803897113211,0.111408414343653,0.0454900280641910,0.0725517536658211,0.0644016234320574,0.0584956389004383,0.126793055589455,0.0908866115465545,0.0411321307334058,0.0692738289380772,0.0708437443424617,0.0548992042850836,0.114285729504047,0.0788135480152052,0.0376176771970332,0.0565434040652236,0.0527665716497745,0.0476913976908450,0.119300694235864,0.147886072362769,0.0337555209440450,0.0582208546838003,0.0505924058911284,0.0460259804262407,0.118988253910350,0.0726684117385397,0.0315050050623462,0.0519481778699604,0.0485046047216217,0.0483969709491359,0.0974894309874196,0.0855519921440915,0.0292154816696315,0.0479901709588337,0.0430890658863303,0.0393903321276035,0.0909267573069019,0.0702170867730463,0.0278219527702299,0.0689434674781626,0.0452192374939217,0.0388722228234852,0.0866758627954328,0.0620812871786083,0.0260113460316605,0.0421830382385265,0.0387943082685463,0.0358006788114896,0.0795932637147764,0.0749930690534258,0.0246725701760771,0.0433951600384139,0.0377430654462939,0.0332792408636930,0.0766025310809220,0.0588704044841320,0.0235955925399967,0.0398907767801552,0.0370076106787303,0.0362398866871031,0.0713440757573736,0.0665466025179797,0.0221191961227829,0.0373217610695905,0.0332517421729284,0.0295270902938340,0.0686008814759274,0.0576770399960665,0.0209268813769427,0.0536315379976979,0.0380812835703351,0.0296019937192688,0.0674915852819815,0.0506378518319878,0.0198819503906200,0.0327970802383811,0.0302082446922441,0.0280565769070661,0.0621814797644564,0.0548947441408895,0.0194334102048846,0.0511925490209149,0.0304522618273720,0.0264489351317071,0.0596115816127902,0.0498354302023666,0.0183720815752653,0.0315552905431255,0.0289393985195708,0.0255784922741505,0.0566968747918586,0.0498746350578228,0.0175928038444230,0.0309626587204910,0.0271979331268442,0.0247771235904147,0.0561000678350336,0.0482263370072755,0.0167509047906884,0.0306836298681903,0.0295136904797102,0.0301887161817878,0.0553833077786568,0.0469881444091836,0.0161950300918126,0.0280696633948006,0.0248654019486431,0.0212116621765097,0.0517212345310446,0.0450108974439320,0.0156613623531573,0.0393065059110886,0.0268695996550433,0.0242034874632327,0.0534247005600975,0.0441303928731280,0.0151212532179802,0.0261282130636768,0.0236273818759760,0.0211302238716508,0.0483247677033497,0.0430018047708304,0.0145408386436545,0.0389057869650058,0.0235949004376349,0.0206741521110715,0.0460210649897785,0.0419087545569386,0.0142782233468975,0.0253619334872626,0.0234076370670243,0.0229241870795940,0.0461003236094212,0.0395260295924286,0.0138448301795670,0.0246822971153225,0.0216397682610892,0.0195688253502099,0.0430752171698957,0.0381070522560722,0.0132563109840366,0.0345561822741686,0.0252006608925133,0.0229907424565463,0.0420830900942783,0.0365552745908592,0.0128910289829759,0.0222236005196593,0.0200912973461312,0.0183283745143871,0.0404258268846344,0.0365844581595456,0.0126340879725718,0.0324620639413316,0.0216805808082700,0.0180141832282019,0.0401191255843743,0.0352586967032804,0.0122055803112268,0.0217208130239961,0.0195478210298138,0.0175157240055103,0.0387476690155897,0.0340665244338038,0.0119338595009010,0.0314754559588272,0.0194926209695145,0.0169808039049446,0.0384534992241625,0.0336010168572037,0.0115774103251644,0.0301973391903957,0.0196979848616612,0.0199931908959458,0.0366832946799787,0.0321486868378579,0.0112530813828470,0.0296923779654012,0.0180146476430734,0.0169568466942015,0.0376917074508066,0.0333351387401858,0.0109073039819467,0.0285258346319584,0.0216457882107305,0.0195064706605696,0.0349950765396196,0.0312712872627659,0.0107184037127667,0.0188700465091009,0.0169935593644321,0.0152685906179230,0.0347179519520870,0.0315213608941155,0.0104559536243444,0.0274666100908269,0.0184395403030014,0.0163684322340871,0.0336271410154620,0.0291546936851201,0.0103382049936376,0.0185273391569489,0.0167610779193276,0.0161827584138184,0.0325404660477993,0.0284934843283116,0.00994925278678341,0.0264597128234477,0.0165792404731140,0.0142159566466314,0.0324828020692176,0.0285033038569324,0.00977700528508094,0.0270761639351920,0.0167757691421617,0.0169750710334952,0.0322739927661327,0.0278115650366688,0.00957783634064658,0.0251800527138816,0.0154847280515556,0.0134191397874395,0.0315802746556976,0.0271900662806948,0.00937421122322141,0.0242958172194686,0.0185562861131863,0.0166022273247081,0.0312562190010846,0.0270215062139014,0.00915838218747557,0.0163165469198270,0.0146791059254116,0.0141442071804363,0.0298957702091218,0.0264584454897542,0.00893763985312985,0.0234934911541286,0.0162397889518126,0.0131374548617133,0.0286473032887305,0.0260229195182037,0.00873698828934518,0.0230109237844625,0.0146006271964759,0.0127700594716558,0.0290294749247222,0.0249593538030056,0.00867029711350995,0.0228426874813668,0.0146974224234290,0.0125358989841179,0.0268052565957549,0.0239295755567404,0.00849973696922565,0.0220848674285994,0.0148034604325694,0.0150992510802916,0.0273257972179152,0.0238732853901964,0.00828111610789272,0.0218637683650602,0.0137996482106061,0.0132717511190369,0.0268412743477370,0.0232957497439944,0.00811092570491636,0.0211768926560976,0.0159703503951248,0.0150210748760231,0.0264272930333729,0.0226365854870749,0.00795619024743068,0.0210289055839998,0.0129720516046906,0.0113281048915063,0.0257925287507495,0.0226267174591728,0.00791979967408489,0.0204434676635900,0.0148377174540664,0.0133472961116309,0.0251813729703272,0.0223978475960224,0.00775442620409063,0.0201873550730969,0.0130807748333761,0.0128714730072783,0.0246128432812693,0.0215448585852668,0.00756535854952169,0.0201037340175707,0.0132437124506879,0.0108039060755587,0.0244935100772315,0.0216641890069636,0.00741538122606615,0.0194715579078440,0.0131800670919955,0.0132084316810890,0.0239428972422362,0.0209952915105509,0.00729263372843204,0.0193285795993192,0.0123033085490358,0.0113176323171634,0.0245211722248032,0.0214426303597227,0.00714609483115586,0.0187678880794944,0.0139200308313409,0.0137049328304858,0.0229785596516036,0.0203006402545530,0.00707189676435824,0.0186619040220006,0.0117488562146934,0.0100923693498908,0.0222495886582748,0.0196189841166998,0.00694893798432048,0.0180809386116389,0.0135660728653243,0.0123379099879740,0.0224165111086159,0.0196292804984855,0.00680511470023437,0.0179829414306312,0.0116433609244045,0.0116712076502994,0.0230064806385155,0.0195685751309968,0.00679550308720253,0.0179921611709940,0.0120520573523409,0.0112648557735797,0.0221473701118188,0.0190259804901316,0.00662714736728971,0.0174153217434285,0.0118792410922819,0.0119463491043253,0.0222284233850171,0.0189569986593411,0.00659876314204988,0.0173171433797013,0.0113092848917238,0.0102595442961945,0.0213679756007036,0.0187145422153635,0.00641933910137340,0.0168533689776484,0.0124494651592826,0.0134442873502279,0.0203410947351749,0.0179671439820678,0.00641702303308976,0.0167765182995985,0.0107392583872162,0.0100418441272656,0.0205769717205021,0.0180606188359185,0.00632732128105611,0.0162977093677025,0.0128731652563376,0.0112899169016350,0.0200045997064156,0.0178236285710944,0.00618559453434285,0.0162132324578271,0.0106695365006391,0.0106414388547316,0.0195745479734150,0.0176128666775710,0.00608983318444651,0.0162886103442102,0.0111788312770270,0.0108527591423869,0.0197570755957475,0.0173210147889876,0.00603664177145397,0.0157481229195956,0.0107667843480706,0.0115694694293289,0.0192072932602050,0.0167523402401350,0.00598162408703552,0.0156944666493778,0.0104500315500638,0.00869918752702094,0.0192549279334858,0.0170630823247449,0.00587221126171008,0.0152963708556715,0.0112851434557191,0.0138985326929529,0.0190680841199657,0.0164630206102400,0.00578500096563830,0.0152418605069725,0.00994032609336375,0.00893497288207774,0.0187527577946502,0.0163215195784633,0.00568021279274697,0.0148546007321515,0.0156536292884337,0.00992054854734246,0.0187090781641469,0.0162902841157773,0.00566118487270783,0.0147605516699547,0.00972651308232302,0.00825660589216017,0.0187483932210098,0.0161582892160823,0.00560498673393789,0.0148461734732133,0.0105441389394452,0.00892087236087181,0.0178505207421212,0.0156510530475579,0.00546082706147610,0.0143738700870872,0.00988934517049537,0.00837412074470527,0.0179444551545752,0.0155057338755742,0.00542455701872545,0.0143583932631219,0.00989154852842195,0.00806284097025337,0.0172482277160552,0.0152929092234910,0.00540760088922871,0.0139927346859935,0.0104531002163711,0.00744578728671298,0.0177806738954098,0.0151719699134844,0.00529528554290738,0.0139650059723539,0.00930007346079130,0.00763940824581528,0.0173532574002316,0.0150274909106590,0.00523180404821391,0.0136161213211966,0.0143783219886104,0.00735476634393024,0.0168799675007447,0.0147189452861415,0.00512531469177738,0.0135451646341051,0.00909889303730167,0.00717687259470160,0.0170771027793238,0.0147717674540675,0.00514683407197119,0.0136040282544694,0.0125076141494779,0.00746052235113134,0.0168230280199414,0.0148239278438290,0.00505190246210728,0.0132183426339741,0.00942601213621858,0.00720259707621030,0.0162092092541085,0.0142883357445182,0.00501555048598031,0.0132367606091543,0.00947719004941255,0.00671070233930744,0.0162995657285549,0.0143682762715256,0.00497211226538670,0.0129004248190315,0.0135404538167744,0.00689752304126516,0.0157716686338333,0.0138468550712362,0.00485397761131488,0.0128862290549038,0.00850895839029396,0.00682709827243405,0.0157853313797894,0.0138499898370057,0.00485581388272643,0.0125804369192988,0.0132367503746168,0.00646499960640746,0.0160563860862572,0.0138618485883055,0.00479332821428841,0.0125121148569109,0.00794540602773179,0.00661201855731945,0.0158426632503910,0.0135727909628957,0.00477185318153346,0.0121423625325515,0.00778485240589310,0.00615822433136518,0.0155706950025644,0.0135953509544725,0.00471722471867608,0.0122130724904145,0.00758345101085547,0.00644726598517334,0.0152441240400296,0.0131690267212254,0.00468388252938771,0.0120765605034441,0.00756642530757339,0.00621613798344276,0.0149536804825575,0.0130386566282805,0.00456550569208802,0.0118711857732349,0.00740334112425530,0.00634801147615309 };
			return dfLUT[(N - 1) * 6 + (m - 1)];
		}
		else
		{
			if (m == 1)
				return 0.2892 * exp(-0.11294 * N) + 0.054895 * exp(-0.013092 * N);
			else if (m == 2)
				return 0.37177 * exp(-0.27004 * N) + 0.08958 * exp(-0.021978 * N);
			else if (m == 3)
				return 0.6658 * exp(-0.4844 * N) + 0.04553 * exp(-0.030793 * N);
			else if (m == 4)
				return 0.50656 * exp(-0.36419 * N) + 0.061509 * exp(-0.01904 * N);
			else if (m == 5)
				return 0.51554 * exp(-0.38706 * N) + 0.060656 * exp(-0.025357 * N);
			else if (m == 6)
				return 0.51908 * exp(-0.40187 * N) + 0.057579 * exp(-0.027425 * N);
		}
	}
	else
		return 1.0 / (1.5 * max(N, m));
}
void initWarpedPFB(WarpedPFB *pfb, double fs, unsigned int N, unsigned int m)
{
	char *memBuf = (char*)(pfb)+sizeof(WarpedPFB);
	unsigned int i, j;
	pfb->noiseLoop = 0;
	unsigned int seed = 1337;
	for (i = 0; i < DENORMAL_BUFFER; i++)
		pfb->noiseBuffer[i] = add_denormal_prevention_white_noise(&seed);
	pfb->N = N;
	pfb->N2 = pfb->N * 2;
	pfb->m = m;
	unsigned int L = 2 * m * N;
	pfb->L = L;
	double df = getOptimalDF(N, m);
	double *h_opt = (double*)malloc(L * sizeof(double));
	cos_fib_paraunitary1(N, m, L, df, h_opt);
	pfb->postGain = (float)(1.0 / (double)(2 * 2 * N));
	double alpha = -(0.1957 - 1.048* sqrt((2.0 / M_PI) * atan(0.07212 * (fs / 1000.0))));
	if (alpha > 0.99)
		alpha = 0.99;
	pfb->alpha = (float)alpha;
	double *f_def = (double*)malloc((N + 1) * sizeof(double));
	pfb->Sk = (unsigned int*)(memBuf);
	subsamplingCal(N, -alpha, f_def, pfb->Sk);
	pfb->freqLabel = (float*)(memBuf + (N * sizeof(unsigned int))); // fcentre
	for (i = 1; i < N - 1; i++)
		pfb->freqLabel[i] = (float)((f_def[i] + f_def[i + 1]) * 0.5 * fs);
	pfb->freqLabel[0] = 0.0f;
	pfb->freqLabel[N - 1] = fs * 0.5;
	free(f_def);
	//for (i = 0; i < N; i++)
	//	printf("%1.7f %d\n", freqLabel[i], Sk[i]);
	unsigned char maximumDownsampling = 1;
	if ((N == 2) && (alpha == 0.0) && maximumDownsampling) // Half band mode
		pfb->Sk[1] = pfb->Sk[0] = 2;
	pfb->virtualHilbertTransformDelay = (float*)(memBuf + (N * sizeof(unsigned int)) + ((N + 1) * sizeof(float))); // fcentre
	for (i = 0; i < N; i++)
	{
		if (pfb->freqLabel[i] > 0.0f)
			pfb->virtualHilbertTransformDelay[i] = (float)(((fs / (double)pfb->Sk[i]) / (double)pfb->freqLabel[i]) / 4.0);
		else
			pfb->virtualHilbertTransformDelay[i] = 0.0f;
	}
	float ovpRatio = 0.0f;
	for (i = 0; i < N; i++)
		ovpRatio += 1.0f / pfb->Sk[i];
	//printf("Total oversampling ratio = %1.3f\n", ovpRatio);
	/*FILE *fp1 = fopen("c.dat", "wb");
	fwrite(C, sizeof(float), CFiltLen, fp1);
	fclose(fp1);*/
	// Analysis preparation
	pfb->allpass_delay_chain = (float*)(memBuf + (N * sizeof(unsigned int)) + ((N + 1) * sizeof(float)) + (N * sizeof(float)));
	memset(pfb->allpass_delay_chain, 0, L * sizeof(float));
	pfb->h = (float*)(memBuf + (N * sizeof(unsigned int)) + ((N + 1) * sizeof(float)) + (N * sizeof(float)) + (L * sizeof(float)));
	for (i = 0; i < m; i++)
	{
		if (((i + 1) % 2) == 0)
		{
			for (j = 0; j < 2 * N; j++)
				pfb->h[i * 2 * N + j] = (float)(-h_opt[i * 2 * N + j]);
		}
		else
		{
			for (j = 0; j < 2 * N; j++)
				pfb->h[i * 2 * N + j] = (float)(h_opt[i * 2 * N + j]);
		}
	}
	free(h_opt);
	//for (i = 0; i < L; i++)
	//	printf("%d %1.7f\n", i + 1, h[i]);
	pfb->channelMatrix = (float*)(memBuf + (N * sizeof(unsigned int)) + ((N + 1) * sizeof(float)) + (N * sizeof(float)) + (L * sizeof(float)) +
		(L * sizeof(float)));
	for (unsigned int k = 0; k < N; k++)
	{
		if ((k % 2) == 0)
		{
			for (unsigned int l = 0; l < 2 * N; l++)
				pfb->channelMatrix[k * (2 * N) + l] = (float)(2.0 * cos((2.0 * k + 1.0) * (M_PI / (2.0 * N)) * ((double)l - (L - 1.0) / 2.0) + (M_PI / 4.0)));
		}
		else
		{
			for (unsigned int l = 0; l < 2 * N; l++)
				pfb->channelMatrix[k * (2 * N) + l] = (float)(2.0 * cos((2.0 * k + 1.0) * (M_PI / (2.0 * N)) * ((double)l - (L - 1.0) / 2.0) - (M_PI / 4.0)));
		}
	}
	//printFloatMatrix2File("res.txt", c, N, 2 * N);
	pfb->decimationCounter = (unsigned int*)(memBuf + (N * sizeof(unsigned int)) + ((N + 1) * sizeof(float)) + (N * sizeof(float)) + (L * sizeof(float)) +
		(L * sizeof(float)) + (N * 2 * N * sizeof(float)));
	for (i = 0; i < N; i++)
		pfb->decimationCounter[i] = pfb->Sk[i];
	pfb->subbandData = (float*)(memBuf + (N * sizeof(unsigned int)) + ((N + 1) * sizeof(float)) + (N * sizeof(float)) + (L * sizeof(float)) +
		(L * sizeof(float)) + (N * 2 * N * sizeof(float)) + (N * sizeof(unsigned int)));
	pfb->APC_delay_1 = (float*)(memBuf + (N * sizeof(unsigned int)) + ((N + 1) * sizeof(float)) + (N * sizeof(float)) + (L * sizeof(float)) +
		(L * sizeof(float)) + (N * 2 * N * sizeof(float)) + (N * sizeof(unsigned int)) + (N * sizeof(float)));
	pfb->APC_delay_2 = (float*)(memBuf + (N * sizeof(unsigned int)) + ((N + 1) * sizeof(float)) + (N * sizeof(float)) + (L * sizeof(float)) +
		(L * sizeof(float)) + (N * 2 * N * sizeof(float)) + (N * sizeof(unsigned int)) + (N * sizeof(float)) +
		(L * sizeof(float)));
	memset(pfb->APC_delay_1, 0, L * sizeof(float));
	memset(pfb->APC_delay_2, 0, L * sizeof(float));
	pfb->Xk2 = (float*)(memBuf + (N * sizeof(unsigned int)) + ((N + 1) * sizeof(float)) + (N * sizeof(float)) + (L * sizeof(float)) +
		(L * sizeof(float)) + (N * 2 * N * sizeof(float)) + (N * sizeof(unsigned int)) + (N * sizeof(float)) +
		(L * sizeof(float)) + (L * sizeof(float)));
}
void assignPtrWarpedPFB(WarpedPFB *pfb, unsigned int N, unsigned int m)
{
	char *memBuf = (char*)(pfb)+sizeof(WarpedPFB);
	unsigned int L = 2 * m * N;
	pfb->allpass_delay_chain = (float*)(memBuf + (N * sizeof(unsigned int)) + ((N + 1) * sizeof(float)) + (N * sizeof(float)));
	memset(pfb->allpass_delay_chain, 0, L * sizeof(float));
	pfb->subbandData = (float*)(memBuf + (N * sizeof(unsigned int)) + ((N + 1) * sizeof(float)) + (N * sizeof(float)) + (L * sizeof(float)) +
		(L * sizeof(float)) + (N * 2 * N * sizeof(float)) + (N * sizeof(unsigned int)));
	pfb->APC_delay_1 = (float*)(memBuf + (N * sizeof(unsigned int)) + ((N + 1) * sizeof(float)) + (N * sizeof(float)) + (L * sizeof(float)) +
		(L * sizeof(float)) + (N * 2 * N * sizeof(float)) + (N * sizeof(unsigned int)) + (N * sizeof(float)));
	pfb->APC_delay_2 = (float*)(memBuf + (N * sizeof(unsigned int)) + ((N + 1) * sizeof(float)) + (N * sizeof(float)) + (L * sizeof(float)) +
		(L * sizeof(float)) + (N * 2 * N * sizeof(float)) + (N * sizeof(unsigned int)) + (N * sizeof(float)) +
		(L * sizeof(float)));
	memset(pfb->APC_delay_1, 0, L * sizeof(float));
	memset(pfb->APC_delay_2, 0, L * sizeof(float));
	pfb->Xk2 = (float*)(memBuf + (N * sizeof(unsigned int)) + ((N + 1) * sizeof(float)) + (N * sizeof(float)) + (L * sizeof(float)) +
		(L * sizeof(float)) + (N * 2 * N * sizeof(float)) + (N * sizeof(unsigned int)) + (N * sizeof(float)) +
		(L * sizeof(float)) + (L * sizeof(float)));
}
void changeWarpingFactorWarpedPFB(WarpedPFB *pfb, float fs, float pfb_log_grid_den)
{
	double alpha = (-(0.1957 - 1.048* sqrt((2.0 / M_PI) * atan(0.07212 * (fs / 1000.0))))) * pfb_log_grid_den;
	if (alpha < -0.99f)
		alpha = -0.99f;
	if (alpha > 0.99f)
		alpha = 0.99f;
	if (pfb->alpha != alpha)
	{
		pfb->alpha = alpha;
		double *f_def = (double*)malloc((pfb->N + 1) * sizeof(double));
		subsamplingCal(pfb->N, -pfb->alpha, f_def, pfb->Sk);
		for (int i = 1; i < pfb->N - 1; i++)
			pfb->freqLabel[i] = (float)((f_def[i] + f_def[i + 1]) * 0.5 * fs);
		pfb->freqLabel[0] = 0.0f;
		pfb->freqLabel[pfb->N - 1] = fs * 0.5;
		free(f_def);
		unsigned char maximumDownsampling = 1;
		if ((pfb->N == 2) && (pfb->alpha == 0.0) && maximumDownsampling) // Half band mode
			pfb->Sk[1] = pfb->Sk[0] = 2;
		for (int i = 0; i < pfb->N; i++)
		{
			if (pfb->freqLabel[i] > 0.0f)
				pfb->virtualHilbertTransformDelay[i] = (float)(((fs / (double)pfb->Sk[i]) / (double)pfb->freqLabel[i]) / 4.0);
			else
				pfb->virtualHilbertTransformDelay[i] = 0.0f;
		}
		float ovpRatio = 0.0f;
		for (int i = 0; i < pfb->N; i++)
			ovpRatio += 1.0f / pfb->Sk[i];
	}
}
void getSubbandDatWarpedPFB(WarpedPFB *pfb, float *subbands, float *curSk)
{
	for (unsigned int i = 0; i < pfb->N; i++)
	{
		subbands[i] = pfb->subbandData[i];
		curSk[i] = (float)pfb->decimationCounter[i];
	}
}
void getSubbandDatWarpedPFBStereo(WarpedPFB *pfb1, WarpedPFB *pfb2, float *subbands1, float *subbands2, float *curSk)
{
	for (unsigned int i = 0; i < pfb1->N; i++)
	{
		subbands1[i] = pfb1->subbandData[i];
		subbands2[i] = pfb2->subbandData[i];
		curSk[i] = (float)pfb1->decimationCounter[i];
	}
}
void writeSubbandDatWarpedPFB(WarpedPFB *pfb, float *subbands)
{
	for (unsigned int i = 0; i < pfb->N; i++)
		pfb->subbandData[i] = subbands[i];
}
void writeSubbandDatWarpedPFBStereo(WarpedPFB *pfb1, WarpedPFB *pfb2, float *subbands1, float *subbands2)
{
	for (int i = 0; i < pfb1->N; i++)
	{
		pfb1->subbandData[i] = subbands1[i];
		pfb2->subbandData[i] = subbands2[i];
	}
}
float* getPhaseCorrFilterWarpedPFB(WarpedPFB *pfb, float phCorrAlpha, unsigned int *CFiltLen)
{
	if (phCorrAlpha < -0.99f)
		phCorrAlpha = -0.99f;
	if (phCorrAlpha > 0.99f)
		phCorrAlpha = 0.99f;
	return allpass_char(pfb->alpha * phCorrAlpha, pfb->L, CFiltLen);
}
void analysisWarpedPFB(WarpedPFB *pfb, float x)
{
	unsigned int i, j;
	// Warping network
	// Complexity: O(n)
	float R1_a = x + pfb->noiseBuffer[pfb->noiseLoop];
	pfb->noiseLoop = (pfb->noiseLoop + 1) & (DENORMAL_BUFFER - 1);
	float R2_a = pfb->allpass_delay_chain[0];
	pfb->allpass_delay_chain[0] = R1_a;
	for (i = 1; i < pfb->L; i++)
	{
		float R3_a = R2_a;
		R2_a = pfb->allpass_delay_chain[i];
		R1_a = (R2_a - R1_a) * pfb->alpha + R3_a;
		pfb->allpass_delay_chain[i] = R1_a;
	}
	// Analysis, multiply by the coefficients of the prototype filter
	// Complexity: O(2 * m * M + 2 * M * sum(1 ./ Sk))
	memset(pfb->subbandData, 0, pfb->N * sizeof(float));
	for (i = 0; i < pfb->N2; i++)
	{
		float Xk = 0.0f;
		for (j = 0; j < pfb->m; j++)
			Xk += pfb->allpass_delay_chain[i + j * pfb->N2] * pfb->h[i + j * pfb->N2];
		//printf("%1.8f\n", Xk[i]);
		// Cosine modulation
		for (j = 0; j < pfb->N; j++)
		{
			if (pfb->decimationCounter[j] == pfb->Sk[j])
				pfb->subbandData[j] += pfb->channelMatrix[j * pfb->N2 + i] * Xk;
		}
	}
	for (j = 0; j < pfb->N; j++)
		pfb->subbandData[j] *= pfb->Sk[j];
}
float synthesisWarpedPFB(WarpedPFB *pfb)
{
	unsigned int i, j;
	// Synthesis
	// Complexity same as analysis
	for (i = 0; i < pfb->N2; i++)
	{
		float Curr_X = 0.0f;
		// Cosine demodulation
		for (j = 0; j < pfb->N; j++)
		{
			if (pfb->decimationCounter[j] == pfb->Sk[j])
				Curr_X += pfb->channelMatrix[j * pfb->N2 + i] * pfb->subbandData[j];
		}
		//printf("%1.8f\n", Curr_X[i]);
		// Polyphase filtering
		for (j = 0; j < pfb->m; j++)
		{
			pfb->Xk2[j * pfb->N2 + i] = Curr_X * pfb->h[j * pfb->N2 + i];
		}
	}
	//for (i = 0; i < L; i++)
	//	printf("%1.8f\n", Xk2[i]);
	// Dewarping network
	// Complexity: O(n)
	float R1_b = pfb->Xk2[0];
	for (i = 0; i < pfb->L - 1; i++)
	{
		float R3_b = pfb->APC_delay_1[i];
		pfb->APC_delay_1[i] = R1_b;
		float R2_b = pfb->APC_delay_2[i];
		R1_b = (R2_b - R1_b) * pfb->alpha + R3_b;
		pfb->APC_delay_2[i] = R1_b;

		R1_b = R1_b + pfb->Xk2[i + 1];
	}
	// Subsampling counter
	for (i = 0; i < pfb->N; i++)
	{
		if (pfb->decimationCounter[i] >= pfb->Sk[i])
			pfb->decimationCounter[i] = 1;
		else
			pfb->decimationCounter[i]++;
	}
	return R1_b * pfb->postGain;
}
void analysisWarpedPFBStereo(WarpedPFB *pfb1, WarpedPFB *pfb2, float *x1, float *x2)
{
	unsigned int i, j;
	// Warping network
	// Complexity: O(n)
	float R1_a = *x1 + pfb1->noiseBuffer[pfb1->noiseLoop];
	float R1_a_x2 = *x2 + pfb1->noiseBuffer[pfb1->noiseLoop];
	pfb1->noiseLoop = (pfb1->noiseLoop + 1) & (DENORMAL_BUFFER - 1);
	float R2_a = pfb1->allpass_delay_chain[0];
	float R2_a_x2 = pfb2->allpass_delay_chain[0];
	pfb1->allpass_delay_chain[0] = R1_a;
	pfb2->allpass_delay_chain[0] = R1_a_x2;
	for (i = 1; i < pfb1->L; i++)
	{
		float R3_a = R2_a;
		float R3_a_x2 = R2_a_x2;
		R2_a = pfb1->allpass_delay_chain[i];
		R2_a_x2 = pfb2->allpass_delay_chain[i];
		R1_a = (R2_a - R1_a) * pfb1->alpha + R3_a;
		R1_a_x2 = (R2_a_x2 - R1_a_x2) * pfb1->alpha + R3_a_x2;
		pfb1->allpass_delay_chain[i] = R1_a;
		pfb2->allpass_delay_chain[i] = R1_a_x2;
	}
	// Analysis, multiply by the coefficients of the prototype filter
	// Complexity: O(2 * m * M + 2 * M * sum(1 ./ Sk))
	memset(pfb1->subbandData, 0, pfb1->N * sizeof(float));
	memset(pfb2->subbandData, 0, pfb1->N * sizeof(float));
	for (i = 0; i < pfb1->N2; i++)
	{
		float Xk = 0.0f;
		float Xk_x2 = 0.0f;
		for (j = 0; j < pfb1->m; j++)
		{
			Xk += pfb1->allpass_delay_chain[i + j * pfb1->N2] * pfb1->h[i + j * pfb1->N2];
			Xk_x2 += pfb2->allpass_delay_chain[i + j * pfb1->N2] * pfb1->h[i + j * pfb1->N2];
		}
		//printf("%1.8f\n", Xk[i]);
		// Cosine modulation
		for (j = 0; j < pfb1->N; j++)
		{
			if (pfb1->decimationCounter[j] == pfb1->Sk[j])
			{
				pfb1->subbandData[j] += pfb1->channelMatrix[j * pfb1->N2 + i] * Xk;
				pfb2->subbandData[j] += pfb1->channelMatrix[j * pfb1->N2 + i] * Xk_x2;
			}
		}
	}
	for (j = 0; j < pfb1->N; j++)
	{
		pfb1->subbandData[j] *= pfb1->Sk[j];
		pfb2->subbandData[j] *= pfb1->Sk[j];
	}
}
void synthesisWarpedPFBStereo(WarpedPFB *pfb1, WarpedPFB *pfb2, float *y1, float *y2)
{
	unsigned int i, j;
	// Synthesis
	// Complexity same as analysis
	for (i = 0; i < pfb1->N2; i++)
	{
		float Curr_X = 0.0f;
		float Curr_X_x2 = 0.0f;
		// Cosine demodulation
		for (j = 0; j < pfb1->N; j++)
		{
			if (pfb1->decimationCounter[j] == pfb1->Sk[j])
			{
				Curr_X += pfb1->channelMatrix[j * pfb1->N2 + i] * pfb1->subbandData[j];
				Curr_X_x2 += pfb1->channelMatrix[j * pfb1->N2 + i] * pfb2->subbandData[j];
			}
		}
		//printf("%1.8f\n", Curr_X[i]);
		// Polyphase filtering
		for (j = 0; j < pfb1->m; j++)
		{
			pfb1->Xk2[j * pfb1->N2 + i] = Curr_X * pfb1->h[j * pfb1->N2 + i];
			pfb2->Xk2[j * pfb1->N2 + i] = Curr_X_x2 * pfb1->h[j * pfb1->N2 + i];
		}
	}
	//for (i = 0; i < L; i++)
	//	printf("%1.8f\n", Xk2[i]);
	// Dewarping network
	// Complexity: O(n)
	float R1_b = pfb1->Xk2[0];
	float R1_b_x2 = pfb2->Xk2[0];
	for (i = 0; i < pfb1->L - 1; i++)
	{
		float R3_b = pfb1->APC_delay_1[i];
		float R3_b_x2 = pfb2->APC_delay_1[i];
		pfb1->APC_delay_1[i] = R1_b;
		pfb2->APC_delay_1[i] = R1_b_x2;
		float R2_b = pfb1->APC_delay_2[i];
		float R2_b_x2 = pfb2->APC_delay_2[i];
		R1_b = (R2_b - R1_b) * pfb1->alpha + R3_b;
		R1_b_x2 = (R2_b_x2 - R1_b_x2) * pfb1->alpha + R3_b_x2;
		pfb1->APC_delay_2[i] = R1_b;
		pfb2->APC_delay_2[i] = R1_b_x2;

		R1_b = R1_b + pfb1->Xk2[i + 1];
		R1_b_x2 = R1_b_x2 + pfb2->Xk2[i + 1];
	}
	// Subsampling counter
	for (i = 0; i < pfb1->N; i++)
	{
		if (pfb1->decimationCounter[i] >= pfb1->Sk[i])
			pfb1->decimationCounter[i] = 1;
		else
			pfb1->decimationCounter[i]++;
	}
	*y1 = R1_b * pfb1->postGain;
	*y2 = R1_b_x2 * pfb1->postGain;
}