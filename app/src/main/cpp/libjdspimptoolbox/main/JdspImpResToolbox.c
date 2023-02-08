#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include "interpolation.h"
void channel_splitFloat(float *buffer, unsigned int num_frames, float **chan_buffers, unsigned int num_channels)
{
	unsigned int i, samples = num_frames * num_channels;
	for (i = 0; i < samples; i++)
		chan_buffers[i % num_channels][i / num_channels] = buffer[i];
}
void channel_joinFloat(float **chan_buffers, unsigned int num_channels, float *buffer, unsigned int num_frames)
{
	unsigned int i, samples = num_frames * num_channels;
	for (i = 0; i < samples; i++)
		buffer[i] = chan_buffers[i % num_channels][i / num_channels];
}
unsigned long upper_power_of_two(unsigned long v)
{
	v--;
	v |= v >> 1;
	v |= v >> 2;
	v |= v >> 4;
	v |= v >> 8;
	v |= v >> 16;
	v++;
	return v;
}
unsigned int LLIntegerLog2(unsigned int v)
{
	unsigned int i = 0;
	while (v > 1)
	{
		++i;
		v >>= 1;
	}
	return i;
}
unsigned LLRevBits(unsigned int x, unsigned int bits)
{
	unsigned int y = 0;
	while (bits--)
	{
		y = (y + y) + (x & 1);
		x >>= 1;
	}
	return y;
}
void LLbitReversalTbl(unsigned *dst, unsigned int n)
{
	unsigned int bits = LLIntegerLog2(n);
	for (unsigned int i = 0; i < n; ++i)
		dst[i] = LLRevBits(i, bits);
}
void LLsinHalfTblFloat(float *dst, unsigned int n)
{
	const double twopi_over_n = 6.283185307179586476925286766559 / n;
	for (unsigned int i = 0; i < n; ++i)
		dst[i] = (float)sin(twopi_over_n * i);
}
void LLdiscreteHartleyFloat(float *A, const unsigned int nPoints, const float *sinTab)
{
	unsigned int i, j, n, n2, theta_inc, nptDiv2;
	float alpha, beta;
	// FHT - stage 1 and 2 (2 and 4 points)
	for (i = 0; i < nPoints; i += 4)
	{
		const float	x0 = A[i];
		const float	x1 = A[i + 1];
		const float	x2 = A[i + 2];
		const float	x3 = A[i + 3];
		const float	y0 = x0 + x1;
		const float	y1 = x0 - x1;
		const float	y2 = x2 + x3;
		const float	y3 = x2 - x3;
		A[i] = y0 + y2;
		A[i + 2] = y0 - y2;
		A[i + 1] = y1 + y3;
		A[i + 3] = y1 - y3;
	}
	// FHT - stage 3 (8 points)
	for (i = 0; i < nPoints; i += 8)
	{
		alpha = A[i];
		beta = A[i + 4];
		A[i] = alpha + beta;
		A[i + 4] = alpha - beta;
		alpha = A[i + 2];
		beta = A[i + 6];
		A[i + 2] = alpha + beta;
		A[i + 6] = alpha - beta;
		alpha = A[i + 1];
		const float beta1 = 0.70710678118654752440084436210485f*(A[i + 5] + A[i + 7]);
		const float beta2 = 0.70710678118654752440084436210485f*(A[i + 5] - A[i + 7]);
		A[i + 1] = alpha + beta1;
		A[i + 5] = alpha - beta1;
		alpha = A[i + 3];
		A[i + 3] = alpha + beta2;
		A[i + 7] = alpha - beta2;
	}
	n = 16;
	n2 = 8;
	theta_inc = nPoints >> 4;
	nptDiv2 = nPoints >> 2;
	while (n <= nPoints)
	{
		for (i = 0; i < nPoints; i += n)
		{
			unsigned int theta = theta_inc;
			const unsigned int n4 = n2 >> 1;
			alpha = A[i];
			beta = A[i + n2];
			A[i] = alpha + beta;
			A[i + n2] = alpha - beta;
			alpha = A[i + n4];
			beta = A[i + n2 + n4];
			A[i + n4] = alpha + beta;
			A[i + n2 + n4] = alpha - beta;
			for (j = 1; j < n4; j++)
			{
				float	sinval = sinTab[theta];
				float	cosval = sinTab[theta + nptDiv2];
				float	alpha1 = A[i + j];
				float	alpha2 = A[i - j + n2];
				float	beta1 = A[i + j + n2] * cosval + A[i - j + n] * sinval;
				float	beta2 = A[i + j + n2] * sinval - A[i - j + n] * cosval;
				theta += theta_inc;
				A[i + j] = alpha1 + beta1;
				A[i + j + n2] = alpha1 - beta1;
				A[i - j + n2] = alpha2 + beta2;
				A[i - j + n] = alpha2 - beta2;
			}
		}
		n <<= 1;
		n2 <<= 1;
		theta_inc >>= 1;
	}
}
typedef struct
{
	unsigned int xLen;
	unsigned int fftLen;
	unsigned int halfLen;
	unsigned int halfLenWdc;
	unsigned int *mBitRev;
	float *mSineTab;
	float threshold, logThreshold, normalizeGain;
} fftData;
void freeMpsFFTData(fftData *fd)
{
	free(fd->mBitRev);
	free(fd->mSineTab);
}
void initMpsFFTData(fftData *fd, unsigned int xLen, float threshdB)
{
	fd->xLen = xLen;
	fd->fftLen = upper_power_of_two(xLen);
	fd->halfLen = fd->fftLen >> 1;
	fd->halfLenWdc = fd->halfLen + 1;
	fd->mBitRev = (unsigned int*)malloc(fd->fftLen * sizeof(unsigned int));
	fd->mSineTab = (float*)malloc(fd->fftLen * sizeof(float));
	LLbitReversalTbl(fd->mBitRev, fd->fftLen);
	LLsinHalfTblFloat(fd->mSineTab, fd->fftLen);
	fd->threshold = powf(10.0f, threshdB / 20.0f);
	fd->logThreshold = logf(fd->threshold);
	fd->normalizeGain = 1.0f / fd->fftLen;
}
void mps(fftData *fd, float *x, float *y)
{
	unsigned int i;
	float *padded = (float*)malloc(fd->fftLen * sizeof(float));
	float *ceptrum = (float*)malloc(fd->fftLen * sizeof(float));
	for (i = 0; i < fd->xLen; i++)
		padded[fd->mBitRev[i]] = x[i];
	for (; i < fd->fftLen; i++)
		padded[fd->mBitRev[i]] = 0.0f;
	LLdiscreteHartleyFloat(padded, fd->fftLen, fd->mSineTab);
	unsigned int symIdx;
	float magnitude = fabsf(padded[0]);
	ceptrum[0] = magnitude < fd->threshold ? fd->logThreshold : logf(magnitude);
	for (i = 1; i < fd->halfLenWdc; i++)
	{
		symIdx = fd->fftLen - i;
		float lR = (padded[i] + padded[symIdx]) * 0.5f;
		float lI = (padded[i] - padded[symIdx]) * 0.5f;
		magnitude = hypotf(lR, lI);
		if (magnitude < fd->threshold)
			ceptrum[fd->mBitRev[i]] = ceptrum[fd->mBitRev[symIdx]] = fd->logThreshold;
		else
		{
			magnitude = logf(magnitude);
			ceptrum[fd->mBitRev[i]] = magnitude;
			ceptrum[fd->mBitRev[symIdx]] = magnitude;
		}
	}
	LLdiscreteHartleyFloat(ceptrum, fd->fftLen, fd->mSineTab);
	padded[0] = ceptrum[0] * fd->normalizeGain;
	padded[fd->mBitRev[fd->halfLen]] = ceptrum[fd->halfLen] * fd->normalizeGain;
	for (i = 1; i < fd->halfLen; i++)
	{
		padded[fd->mBitRev[i]] = (ceptrum[i] + ceptrum[fd->fftLen - i]) * fd->normalizeGain;
		padded[fd->mBitRev[fd->fftLen - i]] = 0.0f;
	}
	LLdiscreteHartleyFloat(padded, fd->fftLen, fd->mSineTab);
	ceptrum[0] = expf(padded[0]);
	float eR;
	for (i = 1; i < fd->halfLenWdc; i++)
	{
		symIdx = fd->fftLen - i;
		float lR = (padded[i] + padded[symIdx]) * 0.5f;
		float lI = (padded[i] - padded[symIdx]) * 0.5f;
		eR = expf(lR);
		lR = eR * cosf(lI);
		lI = eR * sinf(lI);
		ceptrum[fd->mBitRev[i]] = lR + lI;
		ceptrum[fd->mBitRev[symIdx]] = lR - lI;
	}
	LLdiscreteHartleyFloat(ceptrum, fd->fftLen, fd->mSineTab);
	for (i = 0; i < fd->xLen; i++)
		y[i] = ceptrum[i] * fd->normalizeGain;
	free(padded);
	free(ceptrum);
}
#include "cpthread.h"
typedef struct
{
	int rangeMin, rangeMax;
	fftData *fd;
	float **x, **y;
	int sampleShift;
} mpsThread;
void* mpsMulticore(void *args)
{
	mpsThread *th = (mpsThread*)args;
	for (int i = th->rangeMin; i < th->rangeMax; i++)
		mps(th->fd, &th->x[i][th->sampleShift], th->y[i]);
	return 0;
}
void checkStartEnd(float **signal, int channels, int nsamples, float normalizedDbCutoff1, float normalizedDbCutoff2, int range[2])
{
	int i, j;
	float max = fabsf(signal[0][0]);
	for (i = 0; i < channels; i++)
	{
		for (j = 1; j < nsamples; j++)
		{
			if (fabsf(signal[i][j]) > max)
				max = signal[i][j];
		}
	}
	max = 1.0f / ((max < FLT_EPSILON) ? (max + FLT_EPSILON) : max);
	float linGain1 = powf(10.0f, normalizedDbCutoff1 / 20.0f);
	float linGain2 = powf(10.0f, normalizedDbCutoff2 / 20.0f);
	int firstSmps = 0;
	int lastSmps = 0;
	int firstSmpsPrevious = nsamples - 1;
	int lastSmpsPrevious = 0;
	float normalized;
	int found;
	for (i = 0; i < channels; i++)
	{
		found = 0;
		firstSmps = 0;
		for (j = 0; j < nsamples; j++)
		{
			normalized = fabsf(signal[i][j]) * max;
			if (!found)
			{
				if (normalized > linGain1)
				{
					if (!firstSmps)
					{
						firstSmps = j;
						found = 1;
					}
				}
			}
			if (normalized > linGain2)
				lastSmps = j;
		}
		firstSmpsPrevious = (firstSmpsPrevious < firstSmps) ? firstSmpsPrevious : firstSmps;
		lastSmpsPrevious = (lastSmpsPrevious > lastSmps) ? lastSmpsPrevious : lastSmps;
	}
	range[0] = firstSmpsPrevious != (nsamples - 1) ? firstSmpsPrevious : 0;
	range[1] = lastSmpsPrevious + 1;
}
#include "../libsamplerate/samplerate.h"
#define DRMP3_IMPLEMENTATION
#include "../dr_mp3.h"
#define DR_FLAC_IMPLEMENTATION
#include "../dr_flac.h"
#define DR_WAV_IMPLEMENTATION
#include "../dr_wav.h"
const char *get_filename_ext(const char *filename)
{
    const char *dot = strrchr(filename, '.');
    if (!dot || dot == filename) return "";
    return dot + 1;
}
float *decompressedCoefficients = 0;
static const double compressedCoeffMQ[701] = { 0.919063234986138511, 0.913619994199411201, 0.897406560667438402, 0.870768836078722797, 0.834273523109754001, 0.788693711602254766, 0.734989263333015286, 0.674282539592362951, 0.607830143521649657, 0.536991457245508341, 0.463194839157173466, 0.387902406850539450, 0.312574364478514499, 0.238633838900749129, 0.167433166845669668, 0.100222526262645231, 0.038121730693097225, -0.017904091793426027, -0.067064330735278010, -0.108757765594775291, -0.142582153044975485, -0.168338357500518510, -0.186029009837402531, -0.195851834439330574, -0.198187933840591440, -0.193585458951914369, -0.182739217157973949, -0.166466876941489483, -0.145682513177707279, -0.121368299577954988, -0.094545192393702127, -0.066243461643369750, -0.037473912797399318, -0.009200603832691301, 0.017684198632122932, 0.042382162574711987, 0.064207571946511041, 0.082602100079150684, 0.097145355203635028, 0.107561011406507381, 0.113718474453852247, 0.115630166683615837, 0.113444644504459249, 0.107435882084395071, 0.097989162055837783, 0.085584105452548076, 0.070775446120293309, 0.054172207614333223, 0.036415971885660689, 0.018158938330246815, 0.000042459196338912, -0.017323296238410713, -0.033377949403731559, -0.047627263300716267, -0.059656793081079629, -0.069142490141500798, -0.075858019256578396, -0.079678658979652428, -0.080581767609623323, -0.078643906863599317, -0.074034819562284179, -0.067008552930955145, -0.057892102695980191, -0.047072022601671190, -0.034979497375161483, -0.022074413174279148, -0.008828977400685476, 0.004288560713652965, 0.016829555699174666, 0.028379479813981756, 0.038570858162652835, 0.047094241683417769, 0.053706909605020871, 0.058239076113395197, 0.060597464446319166, 0.060766202425508065, 0.058805083502616720, 0.054845323789501445, 0.049083025498695095, 0.041770628243703020, 0.033206689594802247, 0.023724383421121997, 0.013679137601712221, 0.003435850879130631, -0.006643868309165797, -0.016214010012738603, -0.024955612937682017, -0.032586990530198853, -0.038872417226545809, -0.043629018879643239, -0.046731678346964158, -0.048115839004410917, -0.047778163016171563, -0.045775074997070689, -0.042219292770236193, -0.037274512912134725, -0.031148477572630149, -0.024084698830208532, -0.016353156105483747, -0.008240309809337577, -0.000038789761018515, 0.007962880277736915, 0.015490170167632772, 0.022291712611484882, 0.028147455724642705, 0.032875542265754551, 0.036337708303315903, 0.038443049556812471, 0.039150063091460026, 0.038466933347880143, 0.036450092517807633, 0.033201143952433128, 0.028862291652591764, 0.023610467168487866, 0.017650385903971395, 0.011206796641134264, 0.004516210167813600, -0.002181595351151269, -0.008651993358287469, -0.014673562407359826, -0.020045503214184583, -0.024594176093158650, -0.028178551235573571, -0.030694406321622035, -0.032077152831841031, -0.032303222419993387, -0.031389996039150381, -0.029394309376722470, -0.026409616804964359, -0.022561940854659814, -0.018004773700230153, -0.012913130046223239, -0.007476976122050557, -0.001894276500050309, 0.003636091270827173, 0.008921304789011335, 0.013781207467236415, 0.018054338893886482, 0.021603186795815136, 0.024318493648450956, 0.026122487293166251, 0.026970945047679402, 0.026854043263213976, 0.025795987570079431, 0.023853461640206807, 0.021112972713804853, 0.017687209024348168, 0.013710556397414673, 0.009333947668859192, 0.004719238361448204, 0.000033314715823999, -0.004557854609777880, -0.008895014112733140, -0.012831064739959125, -0.016235971633599879, -0.019000975419769615, -0.021041973670496809, -0.022301970824562707, -0.022752529440111541, -0.022394191906072568, -0.021255878361951062, -0.019393302279828196, -0.016886478718542881, -0.013836430536684995, -0.010361223853106050, -0.006591484944463394, -0.002665565936317155, 0.001275464342459697, 0.005092825309417521, 0.008654850008311749, 0.011841465274590917, 0.014548176385701671, 0.016689426206986688, 0.018201223316688792, 0.019042961289876651, 0.019198381208357294, 0.018675660452129358, 0.017506641810096972, 0.015745246837337051, 0.013465145155917528, 0.010756776112709417, 0.007723840062309154, 0.004479392878420811, 0.001141688626311485, -0.002170078649346380, -0.005339982462341837, -0.008259373919338373, -0.010830557217282604, -0.012970007990380254, -0.014611030342508765, -0.015705770153788771, -0.016226526478563909, -0.016166328657139784, -0.015538773207899238, -0.014377140707818779, -0.012732837794565421, -0.010673232279050818, -0.008278969379415602, -0.005640873626063710, -0.002856553527664500, -0.000026834265658038, 0.002747852704083721, 0.005370995258360709, 0.007753319014958258, 0.009815785813909824, 0.011492173003678219, 0.012731150958433296, 0.013497795530424229, 0.013774493273929109, 0.013561219484126362, 0.012875191572278549, 0.011749922250546383, 0.010233717662138146, 0.008387684262046795, 0.006283324310867826, 0.003999812773708582, 0.001621057822818793, -0.000767347236997687, -0.003081171091507831, -0.005240483245491547, -0.007172383140908554, -0.008813427537033921, -0.010111676574189758, -0.011028293954650051, -0.011538653669060464, -0.011632924035784708, -0.011316118830412747, -0.010607624279109693, -0.009540229002217340, -0.008158700990423423, -0.006517970800651516, -0.004680992876389242, -0.002716366825287035, -0.000695807329823454, 0.001308445056270027, 0.003226179724201707, 0.004991648959431359, 0.006545794666321473, 0.007838194454033614, 0.008828664063258869, 0.009488466505815606, 0.009801093167340614, 0.009762597931815446, 0.009381481548192093, 0.008678139395534967, 0.007683900954968532, 0.006439703144240938, 0.004994451750326167, 0.003403135115410580, 0.001724761684323746, 0.000020197792912962, -0.001650015947868542, -0.003227792151864713, -0.004659494079420105, -0.005897735119564774, -0.006902920847777659, -0.007644484483944344, -0.008101778413755888, -0.008264597354555087, -0.008133322264400958, -0.007718687720230902, -0.007041188742854554, -0.006130155461650219, -0.005022535167821778, -0.003761430832691131, -0.002394452762763960, -0.000971945496911797, 0.000454844825025831, 0.001835596641714852, 0.003122668316617104, 0.004272722114380925, 0.005248162177843576, 0.006018339594106877, 0.006560486855847911, 0.006860354383749922, 0.006912532886871314, 0.006720456790559610, 0.006296095343184246, 0.005659348921651181, 0.004837178114662079, 0.003862502033291890, 0.002772909691220670, 0.001609233980906675, 0.000414041581645497, -0.000769906024675906, -0.001901165407831948, -0.002941044990442539, -0.003854910802244932, -0.004613320774623974, -0.005192951162290093, -0.005577286818932973, -0.005757056020769449, -0.005730399990410974, -0.005502776877353867, -0.005086609356845171, -0.004500693886508907, -0.003769397706347888, -0.002921676615038254, -0.001989952181071211, -0.001008891180284735, -0.000014132577398601, 0.000958991766382216, 0.001876697269289887, 0.002707904681562794, 0.003425276863778077, 0.004006100299408528, 0.004432983601002821, 0.004694352366032693, 0.004784727410211850, 0.004704781367912914, 0.004461176612085595, 0.004066195127045020, 0.003537178100163921, 0.002895799341758980, 0.002167201988627001, 0.001379032128141500, 0.000560405874095270, -0.000259152041390146, -0.001050759947470964, -0.001787184184342184, -0.002443762818329620, -0.002999217281793143, -0.003436325772772713, -0.003742437746853627, -0.003909814939345641, -0.003935790838753386, -0.003822747153675574, -0.003577912336230492, -0.003212993416401785, -0.002743658050835511, -0.002188888608264336, -0.001570234143874397, -0.000910989133812205, -0.000235329764723561, 0.000432560641279413, 0.001069345839998360, 0.001653340745377959, 0.002165238082962834, 0.002588733711954676, 0.002911030869493576, 0.003123208352342977, 0.003220442832479460, 0.003202080909987641, 0.003071561941475896, 0.002836197950977609, 0.002506821849485185, 0.002097319592184942, 0.001624065644771860, 0.001105284094542681, 0.000560359841433201, 0.000009125484808694, -0.000528850236297424, -0.001034947274672293, -0.001492128764321782, -0.001885503916298735, -0.002202801129643487, -0.002434736758218434, -0.002575269035034838, -0.002621730990172647, -0.002574840645573092, -0.002438591168737718, -0.002220027862259672, -0.001928922712789991, -0.001577360593112676, -0.001179253996234489, -0.000749805296090004, -0.000304936916893765, 0.000139289578942291, 0.000567244609490058, 0.000964271897894877, 0.001317181565737385, 0.001614678737960774, 0.001847714105275549, 0.002009746006742852, 0.002096907004167219, 0.002108071492117578, 0.002044824491385029, 0.001911335280223238, 0.001714142804308648, 0.001461862761483392, 0.001164828784166054, 0.000834682161765550, 0.000483925998593740, 0.000125460552453497, -0.000227883269383389, -0.000563795658925073, -0.000870909262856999, -0.001139175997920627, -0.001360187135536317, -0.001527426803455576, -0.001636451679881492, -0.001684992470943874, -0.001672975660262524, -0.001602466894019970, -0.001477540114542411, -0.001304079085075351, -0.001089520173828018, -0.000842547115162114, -0.000572749884052837, -0.000290260767615715, -0.000005381173403155, 0.000271787324682131, 0.000531694720882059, 0.000765665077860444, 0.000966179147256847, 0.001127108176071712, 0.001243891947014572, 0.001313656276814221, 0.001335267482640161, 0.001309323638539731, 0.001238084697878249, 0.001125345675261167, 0.000976258990458714, 0.000797113715033597, 0.000595080778704776, 0.000377934148912837, 0.000153758569481225, -0.000069345376995143, -0.000283548328671139, -0.000481561352316614, -0.000656878246049437, -0.000803983100343503, -0.000918516742461215, -0.000997397336828164, -0.001038892182663637, -0.001042639573678904, -0.001009621396079384, -0.000942088876021349, -0.000843445486235168, -0.000718092430499951, -0.000571243299133416, -0.000408715393446694, -0.000236705827693455, -0.000061560820167541, 0.000110453421068778, 0.000273370118506192, 0.000421724138601977, 0.000550730322629083, 0.000656432310144007, 0.000735817450677950, 0.000786894740519778, 0.000808734131933592, 0.000801466989262860, 0.000766248858509478, 0.000705187025332569, 0.000621236516650982, 0.000518069214751335, 0.000399921568730665, 0.000271426983019702, 0.000137439322056229, 0.000002854088234340, -0.000127566289796932, -0.000249356967950712, -0.000358499459727905, -0.000451549869015331, -0.000525742663600061, -0.000579067066332711, -0.000610314210246149, -0.000619094306052033, -0.000605824164738963, -0.000571686465475047, -0.000518563123569660, -0.000448945962885840, -0.000365828604967228, -0.000272584032349123, -0.000172832651673869, -0.000070305865763902, 0.000031289837955523, 0.000128403456337462, 0.000217760218406138, 0.000296468531144650, 0.000362109765670523, 0.000412808249833617, 0.000447279627497663, 0.000464856578836417, 0.000465491738613502, 0.000449738470059130, 0.000418710921836804, 0.000374025488711351, 0.000317726390511799, 0.000252198560764628, 0.000180071382714710, 0.000104117018254314, 0.000027147141711448, -0.000048088182130861, -0.000118995940709898, -0.000183226726188442, -0.000238749615318076, -0.000283913107803554, -0.000317490431754438, -0.000338708143110164, -0.000347257581417331, -0.000343289373546324, -0.000327391777412823, -0.000300554208881314, -0.000264117778630104, -0.000219715066774411, -0.000169201669906033, -0.000114582260151285, -0.000057933995063949, -0.000001330110803372, 0.000053233576939992, 0.000103906741924272, 0.000149044949497856, 0.000187260510974094, 0.000217462319983746, 0.000238883649294571, 0.000251097355835207, 0.000254018413730855, 0.000247894152903089, 0.000233283008194432, 0.000211022966716673, 0.000182191226994878, 0.000148056842795767, 0.000110028310364557, 0.000069598166140896, 0.000028286691831679, -0.000012413223177511, -0.000051088131137235, -0.000086451240238396, -0.000117383287411682, -0.000142965848616867, -0.000162506184310969, -0.000175553056891817, -0.000181903303379713, -0.000181599287597338, -0.000174917679492114, -0.000162350303974188, -0.000144578058136870, -0.000122439106161612, -0.000096892719738482, -0.000068980234721197, -0.000039784640417051, -0.000010390306964679, 0.000018155708707897, 0.000044879452343343, 0.000068911789142910, 0.000089515073669816, 0.000106104020030281, 0.000118260259226232, 0.000125740319217347, 0.000128477011389261, 0.000126574445796332, 0.000120297118433995, 0.000110053709582710, 0.000096376396848920, 0.000079896615190895, 0.000061318285745045, 0.000041389584008291, 0.000020874325790195, 0.000000524017729422, -0.000018948449136533, -0.000036892585232981, -0.000052740818819573, -0.000066025144314650, -0.000076389469747209, -0.000083597404911355, -0.000087535408131387, -0.000088211381288728, -0.000085748963835814, -0.000080377921496540, -0.000072421149619506, -0.000062278911057702, -0.000050411001395778, -0.000037317578835504, -0.000023519411693731, -0.000009538283954733, 0.000004121739654397, 0.000016991550435446, 0.000028652179052461, 0.000038747470023706, 0.000046993903986995, 0.000053187343852112, 0.000057206603209622, 0.000059013855762422, 0.000058652018744224, 0.000056239347370971, 0.000051961567969763, 0.000046061951828715, 0.000038829788015541, 0.000030587750191730, 0.000021678669346899, 0.000012452221695327, 0.000003252019725938, -0.000005596443768274, -0.000013796601731427, -0.000021090036946349, -0.000027263866219030, -0.000032156106624670, -0.000035658928433894, -0.000037719781474555, -0.000038340460301533, -0.000037574245901964, -0.000035521325378456, -0.000032322744186229, -0.000028153186597604, -0.000023212908194301, -0.000017719158939021, -0.000011897436866734, -0.000005972901266780, -0.000000162251446552, 0.000005333655793588, 0.000010336218246169, 0.000014694346087313, 0.000018288433744639, 0.000021032968627464, 0.000022877753957232, 0.000023807775391314, 0.000023841789731823, 0.000023029757133319, 0.000021449274538926, 0.000019201196577477, 0.000016404650185715, 0.000013191660473528, 0.000009701607868611, 0.000006075730729441, 0.000002451874068818, -0.000001040335292299, -0.000004283732788372, -0.000007177155365100, -0.000009638185206925, -0.000011605042076524, -0.000013037603419542, -0.000013917565218600, -0.000014247788012456, -0.000014050900469913, -0.000013367256554309, -0.000012252360976939, -0.000010773890887184, -0.000009008449384796, -0.000007038188493661, -0.000004947435946560, -0.000002819451929447, -0.000000733429418017, 0.000001238164361723, 0.000003031826677860, 0.000004594780305705, 0.000005886220575305, 0.000006878033995645, 0.000007554995005623, 0.000007914466875845, 0.000007965650064675, 0.000007728435880102, 0.000007231934716903, 0.000006512756173231, 0.000005613122895949, 0.000004578901096601, 0.000003457628489583, 0.000002296615219305, 0.000001141185552533, 0.000000033118183302, -0.000000990668545558, -0.000001899152803076, -0.000002667946210802, -0.000003279724008567, -0.000003724353373815, -0.000003998736284580, -0.000004106393367485, -0.000004056823505075, -0.000003864680371659, -0.000003548811395079, -0.000003131206866384, -0.000002635907089885, -0.000002087913711897, -0.000001512147886406, -0.000000932492985725, -0.000000370953442845, 0.000000153045653294, 0.000000623201057861, 0.000001026706448750, 0.000001354458044511, 0.000001601095747237, 0.000001764891111188, 0.000001847498832724, 0.000001853592772907, 0.000001790410657756, 0.000001667233505382, 0.000001494826511430, 0.000001284867642241, 0.000001049388641653, 0.000000800250692844, 0.000000548673760673, 0.000000304834862491, 0.000000077546377014, -0.000000125978796206, -0.000000300272674786, -0.000000441669721214, -0.000000548281621807, -0.000000619897641839, -0.000000657821438861, -0.000000664657100282, -0.000000644058349094, -0.000000600455333568, -0.000000538773213853, -0.000000464155939282, -0.000000381707256690, -0.000000296259201847, -0.000000212176215381, -0.000000133200709942, -0.000000062343516559, 0.0 };
void decompressResamplerMQ(const double y[701], float *yi)
{
	double breaks[701];
	double coefs[2800];
	int32_t k;
	double s[701];
	double dx[700];
	double dvdf[700];
	double r, dzzdx, dzdxdx;
	for (k = 0; k < 700; k++)
	{
		r = 0.0014285714285714286 * ((double)k + 1.0) - 0.0014285714285714286 * (double)k;
		dx[k] = r;
		dvdf[k] = (y[k + 1] - y[k]) / r;
	}
	s[0] = ((dx[0] + 0.0057142857142857143) * dx[1] * dvdf[0] + dx[0] * dx[0] * dvdf[1]) / 0.0028571428571428571;
	s[700] = ((dx[699] + 0.0057142857142857828) * dx[698] * dvdf[699] + dx[699] * dx[699] * dvdf[698]) / 0.0028571428571428914;
	breaks[0] = dx[1];
	breaks[700] = dx[698];
	for (k = 0; k < 699; k++)
	{
		r = dx[k + 1];
		s[k + 1] = 3.0 * (r * dvdf[k] + dx[k] * dvdf[k + 1]);
		breaks[k + 1] = 2.0 * (r + dx[k]);
	}
	r = dx[1] / breaks[0];
	breaks[1] -= r * 0.0028571428571428571;
	s[1] -= r * s[0];
	for (k = 0; k < 698; k++)
	{
		r = dx[k + 2] / breaks[k + 1];
		breaks[k + 2] -= r * dx[k];
		s[k + 2] -= r * s[k + 1];
	}
	r = 0.0028571428571428914 / breaks[699];
	breaks[700] -= r * dx[698];
	s[700] -= r * s[699];
	s[700] /= breaks[700];
	for (k = 698; k >= 0; k--)
		s[k + 1] = (s[k + 1] - dx[k] * s[k + 2]) / breaks[k + 1];
	s[0] = (s[0] - 0.0028571428571428571 * s[1]) / breaks[0];
	for (k = 0; k < 701; k++)
		breaks[k] = 0.0014285714285714286 * (double)k;
	for (k = 0; k < 700; k++)
	{
		r = 1.0 / dx[k];
		dzzdx = (dvdf[k] - s[k]) * r;
		dzdxdx = (s[k + 1] - dvdf[k]) * r;
		coefs[k] = (dzdxdx - dzzdx) * r;
		coefs[k + 700] = 2.0 * dzzdx - dzdxdx;
		coefs[k + 1400] = s[k];
		coefs[k + 2100] = y[k];
	}
	double d = 1.0 / 22437.0;
	int32_t low_i, low_ip1, high_i, mid_i;
	for (k = 0; k < 22438; k++)
	{
		low_i = 0;
		low_ip1 = 2;
		high_i = 701;
		r = k * d;
		while (high_i > low_ip1)
		{
			mid_i = ((low_i + high_i) + 1) >> 1;
			if (r >= breaks[mid_i - 1])
			{
				low_i = mid_i - 1;
				low_ip1 = mid_i + 1;
			}
			else
				high_i = mid_i;
		}
		double xloc = r - breaks[low_i];
		yi[k] = xloc * (xloc * (xloc * coefs[low_i] + coefs[low_i + 700]) + coefs[low_i + 1400]) + coefs[low_i + 2100];
	}
}
void reverse(float *arr, int32_t start, int32_t end)
{
	while (start < end)
	{
		float tmp = arr[start];
		arr[start] = arr[end];
		arr[end] = tmp;
		start++;
		end--;
	}
}
void shift(float *arr, int32_t k, int32_t n)
{
	k = k % n;
	reverse(arr, 0, n - 1);
	reverse(arr, 0, n - k - 1);
	reverse(arr, n - k, n - 1);
}
void circshift(float *x, int n, int k)
{
	k < 0 ? shift(x, -k, n) : shift(x, n - k, n);
}
#define NUMPTS 15
ierper pch1, pch2;
__attribute__((constructor)) static void initialize(void)
{
	if (decompressedCoefficients)
		free(decompressedCoefficients);
	decompressedCoefficients = (float*)malloc(22438 * sizeof(float));
	decompressResamplerMQ(compressedCoeffMQ, decompressedCoefficients);
	initIerper(&pch1, NUMPTS + 2);
	initIerper(&pch2, NUMPTS + 2);
}
__attribute__((destructor)) static void destruction(void)
{
	free(decompressedCoefficients);
	decompressedCoefficients = 0;
	freeIerper(&pch1);
	freeIerper(&pch2);
}
void JamesDSPOfflineResampling(float const *in, float *out, size_t lenIn, size_t lenOut, int channels, double src_ratio, int resampleQuality)
{
	if (lenOut == lenIn && lenIn == 1)
	{
		memcpy(out, in, channels * sizeof(float));
		return;
	}
	SRC_DATA src_data;
	memset(&src_data, 0, sizeof(src_data));
	src_data.data_in = in;
	src_data.data_out = out;
	src_data.input_frames = lenIn;
	src_data.output_frames = lenOut;
	src_data.src_ratio = src_ratio;
	int error;
	if ((error = src_simple(&src_data, resampleQuality, channels)))
	{
//		printf("\n%s\n\n", src_strerror(error));
	}
}
float* loadAudioFile(const char *filename, double targetFs, unsigned int *channels, drwav_uint64 *totalPCMFrameCount, int resampleQuality)
{
	unsigned int fs = 1;
    const char *ext = get_filename_ext(filename);
    float *pSampleData = 0;
    if (!strncmp(ext, "wav", 5) || !strncmp(ext, "irs", 5))
        pSampleData = drwav_open_file_and_read_pcm_frames_f32(filename, channels, &fs, totalPCMFrameCount, 0);
    if (!strncmp(ext, "flac", 5))
        pSampleData = drflac_open_file_and_read_pcm_frames_f32(filename, channels, &fs, totalPCMFrameCount, 0);
    if (!strncmp(ext, "mp3", 5))
    {
        drmp3_config mp3Conf;
        pSampleData = drmp3_open_file_and_read_pcm_frames_f32(filename, &mp3Conf, totalPCMFrameCount, 0);
        *channels = mp3Conf.channels;
        fs = mp3Conf.sampleRate;
    }
	if (pSampleData == NULL)
	{
		printf("Error opening and reading WAV file");
		return 0;
	}
	// Sanity check
	if (*channels < 1)
	{
		printf("Invalid audio channels count");
		free(pSampleData);
		return 0;
	}
	if ((*totalPCMFrameCount <= 0) || (*totalPCMFrameCount <= 0))
	{
		printf("Invalid audio sample rate / frame count");
		free(pSampleData);
		return 0;
	}
	double ratio = targetFs / (double)fs;
	if (ratio != 1.0)
	{
		int compressedLen = (int)ceil(*totalPCMFrameCount * ratio);
		float *tmpBuf = (float*)malloc(compressedLen * *channels * sizeof(float));
		memset(tmpBuf, 0, compressedLen * *channels * sizeof(float));
		JamesDSPOfflineResampling(pSampleData, tmpBuf, *totalPCMFrameCount, compressedLen, *channels, ratio, resampleQuality);
		*totalPCMFrameCount = compressedLen;
		free(pSampleData);
		return tmpBuf;
	}
	return pSampleData;
}

int validateAdvImpParameter(int frameCount, int convMode, jint* advSetPtr, jsize advSetSize) {
	int frameCountGE8 = frameCount < 8 ? 8 : frameCount;
    int splittedBufferSize = convMode == 2 ? (2 * frameCountGE8) : frameCount;
    for(int i = 2; i < advSetSize; i++) {
        if(advSetPtr[i] >= splittedBufferSize || advSetPtr[i] < 0)
            return 0;
    }
    return 1;
}

JNIEXPORT jfloatArray JNICALL Java_me_timschneeberger_rootlessjamesdsp_interop_JdspImpResToolbox_ReadImpulseResponseToFloat
(JNIEnv *env, jobject obj, jstring path, jint targetSampleRate, jintArray jImpInfo, jint convMode, jintArray jadvParam)
{
	const char *mIRFileName = (*env)->GetStringUTFChars(env, path, 0);
	if (strlen(mIRFileName) <= 0) return 0;
	unsigned int channels;
	drwav_uint64 frameCount;
	float *pFrameBuffer = loadAudioFile(mIRFileName, targetSampleRate, &channels, &frameCount, 1);
	if (channels == 0 || channels == 3 || channels > 4)
	{
		free(pFrameBuffer);
		return 0;
	}
	jint *javaAdvSetPtr = (jint*) (*env)->GetIntArrayElements(env, jadvParam, 0);
    jsize javaAdvSetSize = (*env)->GetArrayLength(env, jadvParam);

    if(javaAdvSetSize != 6) {
        return 0;
    }

    int isAdvSetValid = validateAdvImpParameter(frameCount, convMode, javaAdvSetPtr, javaAdvSetSize);
    if(!isAdvSetValid) {
        // Overwrite invalid advanced params
        javaAdvSetPtr[0] = -80;
        javaAdvSetPtr[1] = -100;
        javaAdvSetPtr[2] = 0;
        javaAdvSetPtr[3] = 0;
        javaAdvSetPtr[4] = 0;
        javaAdvSetPtr[5] = 0;
    }

	int i;
	float *splittedBuffer[4];
	int alloc = frameCount;
	if (alloc < 8)
		alloc = 8;
	for (i = 0; i < channels; i++)
	{
		if (convMode == 2)
		{
			splittedBuffer[i] = (float*)malloc(alloc * 2 * sizeof(float));
			memset(splittedBuffer[i], 0, alloc * 2 * sizeof(float));
		}
		else
			splittedBuffer[i] = (float*)malloc(frameCount * sizeof(float));
	}
	channel_splitFloat(pFrameBuffer, frameCount, splittedBuffer, channels);
	if (convMode > 0)
	{
		free(pFrameBuffer);
		int range[2];
		float startCutdB = javaAdvSetPtr[0];
		float endCutdB = javaAdvSetPtr[1];
		if (convMode == 1)
			checkStartEnd(splittedBuffer, channels, frameCount, startCutdB, endCutdB, range);
		else
		{
			range[0] = 0;
			range[1] = alloc * 2;
		}
		float *outPtr[4];
		int xLen = range[1] - range[0];
		if (convMode == 1)
		{
			checkStartEnd(splittedBuffer, channels, frameCount, startCutdB, endCutdB, range);
			for (i = 0; i < channels; i++)
			{
				outPtr[i] = &splittedBuffer[i][range[0]];
				circshift(outPtr[i], xLen, javaAdvSetPtr[i + 2]);
				for (int j = 0; j < javaAdvSetPtr[i + 2] - 1; j++)
					outPtr[i][j] = 0.0f;
			}
		}
		else
		{
			fftData fd;
			initMpsFFTData(&fd, xLen, -80.0f);
			int spawnNthread = channels - 1;
			if (spawnNthread + 1 > channels)
				spawnNthread = channels - 1;
			int taskPerThread = channels / (spawnNthread + 1);
			pthread_t *pthread = (pthread_t*)malloc(spawnNthread * sizeof(pthread_t));
			mpsThread *th = (mpsThread*)malloc(spawnNthread * sizeof(mpsThread));
			for (i = 0; i < spawnNthread; i++)
			{
				th[i].rangeMin = (i + 1) * taskPerThread;
				if (i < spawnNthread - 1)
					th[i].rangeMax = th[i].rangeMin + taskPerThread;
				th[i].fd = &fd;
				th[i].x = splittedBuffer;
				th[i].y = splittedBuffer;
				th[i].sampleShift = range[0];
			}
			th[spawnNthread - 1].rangeMax = channels;
			for (i = 0; i < spawnNthread; i++)
				pthread_create(&pthread[i], 0, mpsMulticore, &th[i]);
			for (i = 0; i < taskPerThread; i++)
				mps(&fd, &splittedBuffer[i][range[0]], splittedBuffer[i]);
			for (i = 0; i < spawnNthread; i++)
				pthread_join(pthread[i], 0);
			free(pthread);
			free(th);
			freeMpsFFTData(&fd);
			checkStartEnd(splittedBuffer, channels, alloc, startCutdB, endCutdB, range);
			xLen = range[1];
			for (i = 0; i < channels; i++)
			{
				outPtr[i] = &splittedBuffer[i][0];
				circshift(outPtr[i], xLen, javaAdvSetPtr[i + 2]);
				for (int j = 0; j < javaAdvSetPtr[i + 2] - 1; j++)
					outPtr[i][j] = 0.0f;
			}
		}
		unsigned int totalFrames = xLen * channels;
		frameCount = xLen;
		pFrameBuffer = (float*)malloc(totalFrames * sizeof(float));
		channel_joinFloat(outPtr, channels, pFrameBuffer, xLen);
	}
	else
	{
		for (i = 0; i < channels; i++)
		{
			circshift(splittedBuffer[i], frameCount, javaAdvSetPtr[i + 2]);
			for (int j = 0; j < javaAdvSetPtr[i + 2] - 1; j++)
				splittedBuffer[i][j] = 0.0f;
		}
		channel_joinFloat(splittedBuffer, channels, pFrameBuffer, frameCount);
	}
	for (i = 0; i < channels; i++)
		free(splittedBuffer[i]);
	(*env)->ReleaseIntArrayElements(env, jadvParam, javaAdvSetPtr, 0);
	jint *javaBasicInfoPtr = (jint*) (*env)->GetIntArrayElements(env, jImpInfo, 0);
	javaBasicInfoPtr[0] = (int)channels;
	javaBasicInfoPtr[1] = (int)frameCount;
	javaBasicInfoPtr[2] = (int)isAdvSetValid;
	(*env)->SetIntArrayRegion(env, jImpInfo, 0, 3, javaBasicInfoPtr);
	jfloatArray outbuf;
	int frameCountTotal = channels * frameCount;
	size_t bufferSize = frameCountTotal * sizeof(float);
	outbuf = (*env)->NewFloatArray(env, (jsize)frameCountTotal);
	(*env)->SetFloatArrayRegion(env, outbuf, 0, (jsize)frameCountTotal, pFrameBuffer);
	free(pFrameBuffer);
	return outbuf;
}
JNIEXPORT jstring JNICALL Java_me_timschneeberger_rootlessjamesdsp_interop_JdspImpResToolbox_OfflineAudioResample
(JNIEnv *env, jobject obj, jstring path, jstring filename, jint targetSampleRate)
{
	const char *jnipath = (*env)->GetStringUTFChars(env, path, 0);
	if (strlen(jnipath) <= 0) return 0;
	const char *mIRFileName = (*env)->GetStringUTFChars(env, filename, 0);
	if (strlen(mIRFileName) <= 0) return 0;
	size_t needed = snprintf(NULL, 0, "%s%s", jnipath, mIRFileName) + 1;
	char *filenameIR = malloc(needed);
	snprintf(filenameIR, needed, "%s%s", jnipath, mIRFileName);
	unsigned int channels;
	drwav_uint64 frameCount;
	float *pFrameBuffer = loadAudioFile(filenameIR, targetSampleRate, &channels, &frameCount, 0);
	free(filenameIR);
	if (!pFrameBuffer)
	{
		needed = snprintf(NULL, 0, "Invalid") + 1;
		filenameIR = malloc(needed);
		snprintf(filenameIR, needed, "Invalid");
	}
	else
	{
		needed = snprintf(NULL, 0, "%s%d_%s", jnipath, targetSampleRate, mIRFileName) + 1;
		filenameIR = malloc(needed);
		snprintf(filenameIR, needed, "%s%d_%s", jnipath, targetSampleRate, mIRFileName);
		drwav pWav;
		drwav_data_format format;
		format.container = drwav_container_riff;
		format.format = DR_WAVE_FORMAT_IEEE_FLOAT;
		format.channels = channels;
		format.sampleRate = targetSampleRate;
		format.bitsPerSample = 32;
		unsigned int fail = drwav_init_file_write(&pWav, filenameIR, &format, 0);
		drwav_uint64 framesWritten = drwav_write_pcm_frames(&pWav, frameCount, pFrameBuffer);
		drwav_uninit(&pWav);
		free(pFrameBuffer);
	}
	(*env)->ReleaseStringUTFChars(env, path, jnipath);
	(*env)->ReleaseStringUTFChars(env, filename, mIRFileName);
	jstring finalName = (*env)->NewStringUTF(env, filenameIR);
	free(filenameIR);
	return finalName;
}
double freq[NUMPTS + 2];
double gain[NUMPTS + 2];
JNIEXPORT jint JNICALL Java_me_timschneeberger_rootlessjamesdsp_interop_JdspImpResToolbox_ComputeEqResponse
(JNIEnv *env, jobject obj, jint n, jdoubleArray jfreq, jdoubleArray jgain, jint interpolationMode, jint queryPts, jdoubleArray dispFreq, jfloatArray response)
{
	jdouble *javaFreqPtr = (jdouble*) (*env)->GetDoubleArrayElements(env, jfreq, 0);
	jdouble *javaGainPtr = (jdouble*) (*env)->GetDoubleArrayElements(env, jgain, 0);
	jdouble *javadispFreqPtr = (jdouble*) (*env)->GetDoubleArrayElements(env, dispFreq, 0);
	jfloat *javaResponsePtr = (jfloat*) (*env)->GetFloatArrayElements(env, response, 0);
	memcpy(freq + 1, javaFreqPtr, NUMPTS * sizeof(double));
	memcpy(gain + 1, javaGainPtr, NUMPTS * sizeof(double));
	(*env)->ReleaseDoubleArrayElements(env, jfreq, javaFreqPtr, 0);
	(*env)->ReleaseDoubleArrayElements(env, jgain, javaGainPtr, 0);
	freq[0] = 0.0;
	gain[0] = gain[1];
	freq[NUMPTS + 1] = 24000.0;
	gain[NUMPTS + 1] = gain[NUMPTS];
	ierper *lerpPtr;
	if (!interpolationMode)
	{
		pchip(&pch1, freq, gain, NUMPTS + 2, 1, 1);
		lerpPtr = &pch1;
	}
	else
	{
		makima(&pch2, freq, gain, NUMPTS + 2, 1, 1);
		lerpPtr = &pch2;
	}
	for (int i = 0; i < queryPts; i++)
	{
		javaResponsePtr[i] = (float)getValueAt(&lerpPtr->cb, javadispFreqPtr[i]);
	}
	(*env)->ReleaseDoubleArrayElements(env, dispFreq, javadispFreqPtr, 0);
	(*env)->SetFloatArrayRegion(env, response, 0, queryPts, javaResponsePtr);
	return 0;
}
