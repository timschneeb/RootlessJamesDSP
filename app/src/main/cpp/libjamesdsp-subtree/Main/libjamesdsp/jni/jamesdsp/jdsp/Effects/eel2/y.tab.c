// A Bison parser, made by GNU Bison 2.3
#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <string.h>
#include <stdint.h>
#include <math.h>
#if ! defined YYSTYPE && ! defined YYSTYPE_IS_DECLARED
typedef int32_t YYSTYPE;
# define yystype YYSTYPE /* obsolescent; will be withdrawn */
# define YYSTYPE_IS_DECLARED 1
# define YYSTYPE_IS_TRIVIAL 1
#endif
#if ! defined YYLTYPE && ! defined YYLTYPE_IS_DECLARED
typedef struct YYLTYPE
{
	int32_t first_line;
	int32_t first_column;
	int32_t last_line;
	int32_t last_column;
} YYLTYPE;
#define yyltype YYLTYPE /* obsolescent; will be withdrawn */
#define YYLTYPE_IS_DECLARED 1
#define YYLTYPE_IS_TRIVIAL 1
#endif
#include "eelCommon.h"
#define YYMALLOC malloc
#define YYFREE free
#define YYTOKENTYPE
// Put the tokens into the symbol table, so that GDB and other debuggers know about them
enum yytokentype {
	VALUE = 258,
	IDENTIFIER = 259,
	TOKEN_SHL = 260,
	TOKEN_SHR = 261,
	TOKEN_LTE = 262,
	TOKEN_GTE = 263,
	TOKEN_EQ = 264,
	TOKEN_EQ_EXACT = 265,
	TOKEN_NE = 266,
	TOKEN_NE_EXACT = 267,
	TOKEN_LOGICAL_AND = 268,
	TOKEN_LOGICAL_OR = 269,
	TOKEN_ADD_OP = 270,
	TOKEN_SUB_OP = 271,
	TOKEN_MOD_OP = 272,
	TOKEN_OR_OP = 273,
	TOKEN_AND_OP = 274,
	TOKEN_XOR_OP = 275,
	TOKEN_DIV_OP = 276,
	TOKEN_MUL_OP = 277,
	TOKEN_POW_OP = 278,
	STRING_LITERAL = 279,
	STRING_IDENTIFIER = 280
};
/* Tokens.  */
#define VALUE 258
#define IDENTIFIER 259
#define TOKEN_SHL 260
#define TOKEN_SHR 261
#define TOKEN_LTE 262
#define TOKEN_GTE 263
#define TOKEN_EQ 264
#define TOKEN_EQ_EXACT 265
#define TOKEN_NE 266
#define TOKEN_NE_EXACT 267
#define TOKEN_LOGICAL_AND 268
#define TOKEN_LOGICAL_OR 269
#define TOKEN_ADD_OP 270
#define TOKEN_SUB_OP 271
#define TOKEN_MOD_OP 272
#define TOKEN_OR_OP 273
#define TOKEN_AND_OP 274
#define TOKEN_XOR_OP 275
#define TOKEN_DIV_OP 276
#define TOKEN_MUL_OP 277
#define TOKEN_POW_OP 278
#define STRING_LITERAL 279
#define STRING_IDENTIFIER 280
static const char *nseel_skip_space_and_comments(const char *p, const char *endptr)
{
	for (;;)
	{
		while (p < endptr && isspace(p[0])) p++;
		if (p >= endptr - 1 || *p != '/') return p;
		if (p[1] == '/')
		{
			while (p < endptr && *p != '\r' && *p != '\n') p++;
		}
		else if (p[1] == '*')
		{
			p += 2;
			while (p < endptr - 1 && (p[0] != '*' || p[1] != '/')) p++;
			p += 2;
			if (p >= endptr) return endptr;
		}
		else return p;
	}
}
// removes any escaped characters, also will convert pairs delim_char into single delim_chars
int32_t nseel_filter_escaped_string(char *outbuf, int32_t outbuf_sz, const char *rdptr, size_t rdptr_size, char delim_char)
{
	int32_t outpos = 0;
	const char *rdptr_end = rdptr + rdptr_size;
	while (rdptr < rdptr_end && outpos < outbuf_sz - 1)
	{
		char thisc = *rdptr;
		if (thisc == '\\' && rdptr < rdptr_end - 1)
		{
			const char nc = rdptr[1];
			if (nc == 'r' || nc == 'R') { thisc = '\r'; }
			else if (nc == 'n' || nc == 'N') { thisc = '\n'; }
			else if (nc == 't' || nc == 'T') { thisc = '\t'; }
			else if (nc == 'b' || nc == 'B') { thisc = '\b'; }
			else if ((nc >= '0' && nc <= '9') || nc == 'x' || nc == 'X')
			{
				unsigned char c = 0;
				char base_shift = 3;
				char num_top = '7';
				rdptr++; // skip backslash
				if (nc > '9') // implies xX
				{
					base_shift = 4;
					num_top = '9';
					rdptr++; // skip x
				}
				while (rdptr < rdptr_end)
				{
					char tc = *rdptr;
					if (tc >= '0' && tc <= num_top)
					{
						c = (c << base_shift) + tc - '0';
					}
					else if (base_shift == 4)
					{
						if (tc >= 'a' && tc <= 'f')
						{
							c = (c << base_shift) + (tc - 'a' + 10);
						}
						else if (tc >= 'A' && tc <= 'F')
						{
							c = (c << base_shift) + (tc - 'A' + 10);
						}
						else break;
					}
					else break;
					rdptr++;
				}
				outbuf[outpos++] = (char)c;
				continue;
			}
			else  // \c where c is an unknown character drops the backslash -- works for \, ', ", etc
			{
				thisc = nc;
			}
			rdptr += 2;
		}
		else
		{
			if (thisc == delim_char) break;
			rdptr++;
		}
		outbuf[outpos++] = thisc;
	}
	outbuf[outpos] = 0;
	return outpos;
}
// state can be NULL, it will be set if finished with unterminated thing: 1 for multiline comment, ' or " for string
const char *nseel_simple_tokenizer(const char **ptr, const char *endptr, int32_t *lenOut, int32_t *state)
{
	const char *p = *ptr;
	const char *rv = p;
	char delim;
	if (state) // if state set, returns comments as tokens
	{
		if (*state == 1)
			goto in_comment;
		if (*state == '\'' || *state == '\"')
		{
			delim = (char)*state;
			goto in_string;
		}
		// skip any whitespace
		while (p < endptr && isspace(p[0])) p++;
	}
	else
	{
		// state not passed, skip comments (do not return them as tokens)
		p = nseel_skip_space_and_comments(p, endptr);
	}
	if (p >= endptr)
	{
		*ptr = endptr;
		*lenOut = 0;
		return NULL;
	}
	rv = p;
	if (*p == '$' && p + 3 < endptr && p[1] == '\'' && p[3] == '\'')
	{
		p += 4;
	}
	else if (state && *p == '/' && p < endptr - 1 && (p[1] == '/' || p[1] == '*'))
	{
		if (p[1] == '/')
		{
			while (p < endptr && *p != '\r' && *p != '\n') p++; // advance to end of line
		}
		else
		{
			if (state) *state = 1;
			p += 2;
		in_comment:
			while (p < endptr)
			{
				const char c = *p++;
				if (c == '*' && p < endptr && *p == '/')
				{
					p++;
					if (state) *state = 0;
					break;
				}
			}
		}
	}
	else if (isalnum(*p) || *p == '_' || *p == '#' || *p == '$')
	{
		if (*p == '$' && p < endptr - 1 && p[1] == '~') p++;
		p++;
		while (p < endptr && (isalnum(*p) || *p == '_' || *p == '.')) p++;
	}
	else if (*p == '\'' || *p == '\"')
	{
		delim = *p++;
		if (state) *state = delim;
	in_string:
		while (p < endptr)
		{
			const char c = *p++;
			if (p < endptr && c == '\\') p++;  // skip escaped characters
			else if (c == delim)
			{
				if (state) *state = 0;
				break;
			}
		}
	}
	else
	{
		p++;
	}
	*ptr = p;
	*lenOut = (int32_t)(p - rv);
	return p > rv ? rv : NULL;
}
int32_t nseellex(opcodeRec **output, YYLTYPE * yylloc_param, compileContext *scctx)
{
	int32_t rv = 0, toklen = 0;
	const char *rdptr = scctx->rdbuf;
	const char *endptr = scctx->rdbuf_end;
	const char *tok = nseel_simple_tokenizer(&rdptr, endptr, &toklen, NULL);
	*output = 0;
	if (tok)
	{
		rv = tok[0];
		if (rv == '$')
		{
			if (rdptr != tok + 1)
			{
				*output = nseel_translate(scctx, tok, rdptr - tok);
				if (*output) rv = VALUE;
			}
		}
		else if (rv == '\'')
		{
			if (toklen > 1 && tok[toklen - 1] == '\'')
			{
				*output = nseel_translate(scctx, tok, toklen);
				if (*output) rv = VALUE;
			}
			else scctx->gotEndOfInput |= 8;
		}
		else if (rv == '\"' && scctx->onString)
		{
			if (toklen > 1 && tok[toklen - 1] == '\"')
			{
				*output = (opcodeRec *)nseel_createStringSegmentRec(scctx, tok, toklen);
				if (*output) rv = STRING_LITERAL;
			}
			else scctx->gotEndOfInput |= 16;
		}
		else if (isalpha(rv) || rv == '_')
		{
			// toklen already valid
			char buf[NSEEL_MAX_VARIABLE_NAMELEN * 2];
			if (toklen > sizeof(buf) - 1) toklen = sizeof(buf) - 1;
			memcpy(buf, tok, toklen);
			buf[toklen] = 0;
			*output = nseel_createCompiledValuePtr(scctx, NULL, buf);
			if (*output) rv = IDENTIFIER;
		}
		else if ((rv >= '0' && rv <= '9') || (rv == '.' && (rdptr < endptr && rdptr[0] >= '0' && rdptr[0] <= '9')))
		{
			if (rv == '0' && rdptr < endptr && (rdptr[0] == 'x' || rdptr[0] == 'X'))
			{
				rdptr++;
				while (rdptr < endptr && (rv = rdptr[0]) && ((rv >= '0' && rv <= '9') || (rv >= 'a' && rv <= 'f') || (rv >= 'A' && rv <= 'F'))) rdptr++;
			}
			else
			{
				int32_t pcnt = rv == '.';
				while (rdptr < endptr && (rv = rdptr[0]) && ((rv >= '0' && rv <= '9') || (rv == '.' && !pcnt++))) rdptr++;
			}
			*output = nseel_translate(scctx, tok, rdptr - tok);
			if (*output) rv = VALUE;
		}
		else if (rv == '<')
		{
			const char nc = *rdptr;
			if (nc == '<')
			{
				rdptr++;
				rv = TOKEN_SHL;
			}
			else if (nc == '=')
			{
				rdptr++;
				rv = TOKEN_LTE;
			}
		}
		else if (rv == '>')
		{
			const char nc = *rdptr;
			if (nc == '>')
			{
				rdptr++;
				rv = TOKEN_SHR;
			}
			else if (nc == '=')
			{
				rdptr++;
				rv = TOKEN_GTE;
			}
		}
		else if (rv == '&' && *rdptr == '&')
		{
			rdptr++;
			rv = TOKEN_LOGICAL_AND;
		}
		else if (rv == '|' && *rdptr == '|')
		{
			rdptr++;
			rv = TOKEN_LOGICAL_OR;
		}
		else if (*rdptr == '=')
		{
			switch (rv)
			{
			case '+': rv = TOKEN_ADD_OP; rdptr++; break;
			case '-': rv = TOKEN_SUB_OP; rdptr++; break;
			case '%': rv = TOKEN_MOD_OP; rdptr++; break;
			case '|': rv = TOKEN_OR_OP;  rdptr++; break;
			case '&': rv = TOKEN_AND_OP; rdptr++; break;
			case '~': rv = TOKEN_XOR_OP; rdptr++; break;
			case '/': rv = TOKEN_DIV_OP; rdptr++; break;
			case '*': rv = TOKEN_MUL_OP; rdptr++; break;
			case '^': rv = TOKEN_POW_OP; rdptr++; break;
			case '!':
				rdptr++;
				if (rdptr < endptr && *rdptr == '=')
				{
					rdptr++;
					rv = TOKEN_NE_EXACT;
				}
				else
					rv = TOKEN_NE;
				break;
			case '=':
				rdptr++;
				if (rdptr < endptr && *rdptr == '=')
				{
					rdptr++;
					rv = TOKEN_EQ_EXACT;
				}
				else
					rv = TOKEN_EQ;
				break;
			}
		}
	}
	scctx->rdbuf = rdptr;
	yylloc_param->first_column = (int32_t)(tok - scctx->rdbuf_start);
	return rv;
}
void nseelerror(YYLTYPE *pos, compileContext *ctx, const char *str)
{
	ctx->errVar = pos->first_column > 0 ? pos->first_column : (int32_t)(ctx->rdbuf_end - ctx->rdbuf_start);
}
/* Substitute the variable and function names.  */
#define yyparse nseelparse
#define yylex   nseellex
#define yyerror nseelerror
#define yylval  nseellval
#define yychar  nseelchar
#define yydebug nseeldebug
#define yynerrs nseelnerrs
#define yylloc nseellloc
/* Copy the first part of user declarations.  */
#line 13 "eel2.y"
#define scanner context->scanner
#define YY_(x) ("")
/* Enabling traces.  */
#ifndef YYDEBUG
# define YYDEBUG 0
#endif
/* Enabling verbose error messages.  */
#ifdef YYERROR_VERBOSE
# undef YYERROR_VERBOSE
# define YYERROR_VERBOSE 1
#else
# define YYERROR_VERBOSE 0
#endif
/* Enabling the token table.  */
#ifndef YYTOKEN_TABLE
# define YYTOKEN_TABLE 0
#endif
#if ! defined YYSTYPE && ! defined YYSTYPE_IS_DECLARED
typedef int32_t YYSTYPE;
# define yystype YYSTYPE /* obsolescent; will be withdrawn */
# define YYSTYPE_IS_DECLARED 1
# define YYSTYPE_IS_TRIVIAL 1
#endif
#if ! defined YYLTYPE && ! defined YYLTYPE_IS_DECLARED
typedef struct YYLTYPE
{
  int32_t first_line;
  int32_t first_column;
  int32_t last_line;
  int32_t last_column;
} YYLTYPE;
# define yyltype YYLTYPE /* obsolescent; will be withdrawn */
# define YYLTYPE_IS_DECLARED 1
# define YYLTYPE_IS_TRIVIAL 1
#endif
/* Copy the second part of user declarations.  */
/* Line 216 of yacc.c.  */
#ifndef YYSIZE_T
# ifdef __SIZE_TYPE__
#  define YYSIZE_T __SIZE_TYPE__
# elif defined size_t
#  define YYSIZE_T size_t
# elif ! defined YYSIZE_T && (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
#  include <stddef.h> /* INFRINGES ON USER NAME SPACE */
#  define YYSIZE_T size_t
# else
#  define YYSIZE_T uint32_t
# endif
#endif
#define YYSIZE_MAXIMUM ((YYSIZE_T) -1)
#ifndef YY_
# if defined YYENABLE_NLS && YYENABLE_NLS
#  if ENABLE_NLS
#   include <libintl.h> /* INFRINGES ON USER NAME SPACE */
#   define YY_(msgid) dgettext ("bison-runtime", msgid)
#  endif
# endif
# ifndef YY_
#  define YY_(msgid) msgid
# endif
#endif
/* Suppress unused-variable warnings by "using" E.  */
#if ! defined lint || defined __GNUC__
# define YYUSE(e) ((void) (e))
#else
# define YYUSE(e) /* empty */
#endif
/* Identity function, used to suppress warnings about constant conditions.  */
#ifndef lint
# define YYID(n) (n)
#else
#if (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
static int32_t
YYID (int32_t i)
#else
static int32_t
YYID (i)
    int32_t i;
#endif
{
  return i;
}
#endif
#if ! defined yyoverflow || YYERROR_VERBOSE
/* The parser invokes alloca or malloc; define the necessary symbols.  */
#  define YYSTACK_ALLOC YYMALLOC
#  define YYSTACK_FREE YYFREE
#  ifndef YYSTACK_ALLOC_MAXIMUM
#   define YYSTACK_ALLOC_MAXIMUM YYSIZE_MAXIMUM
#  endif
#  if (defined __cplusplus && ! defined _STDLIB_H \
       && ! ((defined YYMALLOC || defined malloc) \
	     && (defined YYFREE || defined free)))
#   include <stdlib.h> /* INFRINGES ON USER NAME SPACE */
#   ifndef _STDLIB_H
#    define _STDLIB_H 1
#   endif
#  endif
#  ifndef YYMALLOC
#   define YYMALLOC malloc
#   if ! defined malloc && ! defined _STDLIB_H && (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
void *malloc(YYSIZE_T); /* INFRINGES ON USER NAME SPACE */
#   endif
#  endif
#  ifndef YYFREE
#   define YYFREE free
#   if ! defined free && ! defined _STDLIB_H && (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
void free(void *); /* INFRINGES ON USER NAME SPACE */
#   endif
#  endif
# endif
#if (! defined yyoverflow \
     && (! defined __cplusplus \
	 || (defined YYLTYPE_IS_TRIVIAL && YYLTYPE_IS_TRIVIAL \
	     && defined YYSTYPE_IS_TRIVIAL && YYSTYPE_IS_TRIVIAL)))
/* A type that is properly aligned for any stack member.  */
union yyalloc
{
  int16_t yyss;
  YYSTYPE yyvs;
    YYLTYPE yyls;
};
/* The size of the maximum gap between one aligned stack and the next.  */
# define YYSTACK_GAP_MAXIMUM (sizeof (union yyalloc) - 1)
/* The size of an array large to enough to hold all stacks, each with
   N elements.  */
# define YYSTACK_BYTES(N) \
     ((N) * (sizeof (int16_t) + sizeof (YYSTYPE) + sizeof (YYLTYPE)) \
      + 2 * YYSTACK_GAP_MAXIMUM)
/* Copy COUNT objects from FROM to TO.  The source and destination do
   not overlap.  */
# ifndef YYCOPY
#  if defined __GNUC__ && 1 < __GNUC__
#   define YYCOPY(To, From, Count) \
      __builtin_memcpy (To, From, (Count) * sizeof (*(From)))
#  else
#   define YYCOPY(To, From, Count)		\
      do					\
	{					\
	  YYSIZE_T yyi;				\
	  for (yyi = 0; yyi < (Count); yyi++)	\
	    (To)[yyi] = (From)[yyi];		\
	}					\
      while (YYID (0))
#  endif
# endif
/* Relocate STACK from its old location to the new one.  The
   local variables YYSIZE and YYSTACKSIZE give the old and new number of
   elements in the stack, and YYPTR gives the new location of the
   stack.  Advance YYPTR to a properly aligned location for the next
   stack.  */
# define YYSTACK_RELOCATE(Stack)					\
    do									\
      {									\
	YYSIZE_T yynewbytes;						\
	YYCOPY (&yyptr->Stack, Stack, yysize);				\
	Stack = &yyptr->Stack;						\
	yynewbytes = yystacksize * sizeof (*Stack) + YYSTACK_GAP_MAXIMUM; \
	yyptr += yynewbytes / sizeof (*yyptr);				\
      }									\
    while (YYID (0))
#endif
/* YYFINAL -- State number of the termination state.  */
#define YYFINAL  68
/* YYLAST -- Last index in YYTABLE.  */
#define YYLAST   141
/* YYNTOKENS -- Number of terminals.  */
#define YYNTOKENS  47
/* YYNNTS -- Number of nonterminals.  */
#define YYNNTS  19
/* YYNRULES -- Number of rules.  */
#define YYNRULES  73
/* YYNRULES -- Number of states.  */
#define YYNSTATES  127
/* YYTRANSLATE(YYLEX) -- Bison symbol number corresponding to YYLEX.  */
#define YYUNDEFTOK  2
#define YYMAXUTOK   280
#define YYTRANSLATE(YYX)						\
  ((uint32_t) (YYX) <= YYMAXUTOK ? yytranslate[YYX] : YYUNDEFTOK)
/* YYTRANSLATE[YYLEX] -- Bison symbol number corresponding to YYLEX.  */
static const uint8_t yytranslate[] =
{
       0,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,    34,     2,     2,     2,    36,    39,     2,
      27,    28,    38,    32,    26,    33,     2,    37,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,    45,    46,
      42,    31,    43,    44,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,    29,     2,    30,    35,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,    40,     2,    41,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     2,     2,     2,     2,
       2,     2,     2,     2,     2,     2,     1,     2,     3,     4,
       5,     6,     7,     8,     9,    10,    11,    12,    13,    14,
      15,    16,    17,    18,    19,    20,    21,    22,    23,    24,
      25
};
/* YYR1[YYN] -- Symbol number of symbol that rule YYN derives.  */
static const uint8_t yyr1[] =
{
       0,    47,    48,    48,    49,    49,    50,    50,    50,    50,
      50,    50,    50,    50,    50,    51,    51,    51,    51,    52,
      52,    52,    52,    52,    52,    52,    52,    52,    52,    52,
      52,    52,    53,    53,    53,    53,    54,    54,    55,    55,
      55,    55,    56,    56,    57,    57,    58,    58,    59,    59,
      60,    60,    60,    60,    61,    61,    61,    61,    61,    61,
      61,    61,    61,    62,    62,    62,    63,    63,    63,    63,
      64,    64,    64,    65
};
/* YYR2[YYN] -- Number of symbols composing right hand side of rule YYN.  */
static const uint8_t yyr2[] =
{
       0,     2,     1,     3,     1,     2,     1,     3,     7,     4,
       3,     6,     8,     3,     4,     1,     1,     1,     1,     1,
       3,     3,     3,     3,     3,     3,     3,     3,     3,     3,
       3,     3,     1,     2,     2,     2,     1,     3,     1,     3,
       3,     3,     1,     3,     1,     3,     1,     3,     1,     3,
       1,     3,     3,     3,     1,     3,     3,     3,     3,     3,
       3,     3,     3,     1,     3,     3,     1,     5,     4,     3,
       1,     3,     2,     1
};
/* YYDEFACT[STATE-NAME] -- Default rule to reduce with in state
   STATE-NUM when YYTABLE doesn't specify something else to do.  Zero
   means the default is an error.  */
static const uint8_t yydefact[] =
{
       0,    15,     6,     4,    16,     0,     0,     0,     0,    17,
      18,    19,    32,    36,    38,    42,    44,    46,    48,    50,
      54,    63,    66,    70,    73,     0,     0,     5,     0,     0,
       0,    33,    34,    35,     0,     0,     0,     0,     0,     0,
       0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
       0,     0,     0,     0,     0,     0,     0,     0,     0,     0,
       0,     0,     0,     0,     0,     0,     0,    72,     1,    10,
       0,    31,    30,     7,    21,    22,    23,    24,    25,    26,
      27,    28,    29,    20,    13,     0,    37,    40,    41,    39,
      43,    45,    47,    49,    51,    52,    53,    57,    58,    59,
      60,    61,    62,    55,    56,    64,    65,     0,    69,    71,
       0,     9,    14,    68,     0,     0,     0,    67,     0,    11,
       0,     0,     2,     8,    12,     0,     3
};
/* YYDEFGOTO[NTERM-NUM].  */
static const int8_t yydefgoto[] =
{
      -1,   121,     9,    10,    11,    12,    13,    14,    15,    16,
      17,    18,    19,    20,    21,    22,    23,   122,    25
};
/* YYPACT[STATE-NUM] -- Index in YYTABLE of the portion describing
   STATE-NUM.  */
#define YYPACT_NINF -38
static const int8_t yypact[] =
{
      70,   -38,   -21,    12,   -11,    70,    70,    70,    70,   -38,
     102,     6,   -38,   -38,    50,    19,     4,     8,    14,    25,
      51,    22,    40,   -38,    47,    96,    34,   -38,    70,    70,
      43,   -38,   -38,   -38,    70,    70,    70,    70,    70,    70,
      70,    70,    70,    70,    45,    70,    70,    70,    70,    70,
      70,    70,    70,    70,    70,    70,    70,    70,    70,    70,
      70,    70,    70,    70,    70,    70,    18,    70,   -38,   -38,
      55,   -38,   -38,   -38,   -38,   -38,   -38,   -38,   -38,   -38,
     -38,   -38,   -38,   -38,   -38,    30,   -38,    50,    50,    50,
      19,     4,     8,    14,    25,    25,    25,    51,    51,    51,
      51,    51,    51,    51,    51,    22,    22,    70,    53,   -38,
      70,    72,   -38,   -38,    70,    60,    70,   -38,    70,   -38,
      54,    77,   -23,   -38,   -38,    70,   -38
};
/* YYPGOTO[NTERM-NUM].  */
static const int8_t yypgoto[] =
{
     -38,   -10,   111,   -38,   -38,   -38,    11,    61,    79,    76,
      80,    75,    58,    78,   -37,   -38,   -27,     0,   -38
};
/* YYTABLE[YYPACT[STATE-NUM]].  What to do in state STATE-NUM.  If
   positive, shift that token.  If negative, reduce the rule which
   number is the opposite.  If zero, do what YYDEFACT says.
   If YYTABLE_NINF, syntax error.  */
#define YYTABLE_NINF -1
static const uint8_t yytable[] =
{
      24,    71,    72,   125,    28,    30,    26,    74,    75,    76,
      77,    78,    79,    80,    81,    82,    83,    31,    32,    33,
      29,     1,     2,    67,    46,    47,    70,   105,   106,    56,
      57,    58,    59,    60,    61,    44,     3,     1,     2,   108,
     109,    49,     3,     4,    85,     5,    50,    51,     1,     2,
       6,     7,     8,    64,    65,    48,    86,    52,     3,     4,
     112,     5,    69,   107,    62,    63,     6,     7,     8,     3,
       4,    73,     5,     1,     2,    84,    67,     6,     7,     8,
     113,   110,   123,   111,    66,    45,   118,   117,   119,    67,
      53,    54,    55,    67,     3,     4,    68,     5,   114,   116,
      67,    67,     6,     7,     8,   124,    67,    87,    88,    89,
     115,    94,    95,    96,    27,   126,   120,    34,    35,    36,
      37,    38,    39,    40,    41,    42,    91,    93,    90,     0,
       0,    92,     0,    43,    97,    98,    99,   100,   101,   102,
     103,   104
};
static const int8_t yycheck[] =
{
       0,    28,    29,    26,    15,     5,    27,    34,    35,    36,
      37,    38,    39,    40,    41,    42,    43,     6,     7,     8,
      31,     3,     4,    46,     5,     6,    26,    64,    65,     7,
       8,     9,    10,    11,    12,    29,    24,     3,     4,    66,
      67,    37,    24,    25,    44,    27,    38,    33,     3,     4,
      32,    33,    34,    13,    14,    36,    45,    32,    24,    25,
      30,    27,    28,    45,    42,    43,    32,    33,    34,    24,
      25,    28,    27,     3,     4,    30,    46,    32,    33,    34,
     107,    26,    28,    28,    44,    35,    26,   114,    28,    46,
      39,    40,    41,    46,    24,    25,     0,    27,    45,    27,
      46,    46,    32,    33,    34,    28,    46,    46,    47,    48,
     110,    53,    54,    55,     3,   125,   116,    15,    16,    17,
      18,    19,    20,    21,    22,    23,    50,    52,    49,    -1,
      -1,    51,    -1,    31,    56,    57,    58,    59,    60,    61,
      62,    63
};
#define yyerrok		(yyerrstatus = 0)
#define yyclearin	(yychar = YYEMPTY)
#define YYEMPTY		(-2)
#define YYEOF		0
#define YYACCEPT	goto yyacceptlab
#define YYABORT		goto yyabortlab
#define YYERROR		goto yyerrorlab
/* Like YYERROR except do call yyerror.  This remains here temporarily
   to ease the transition to the new meaning of YYERROR, for GCC.
   Once GCC version 2 has supplanted version 1, this can go.  */
#define YYFAIL		goto yyerrlab
#define YYRECOVERING()  (!!yyerrstatus)
#define YYBACKUP(Token, Value)					\
do								\
  if (yychar == YYEMPTY && yylen == 1)				\
    {								\
      yychar = (Token);						\
      yylval = (Value);						\
      yytoken = YYTRANSLATE (yychar);				\
      YYPOPSTACK (1);						\
      goto yybackup;						\
    }								\
  else								\
    {								\
      yyerror (&yylloc, context, YY_("syntax error: cannot back up")); \
      YYERROR;							\
    }								\
while (YYID (0))
#define YYTERROR	1
#define YYERRCODE	256
/* YYLLOC_DEFAULT -- Set CURRENT to span from RHS[1] to RHS[N].
   If N is 0, then set CURRENT to the empty location which ends
   the previous symbol: RHS[0] (always defined).  */
#define YYRHSLOC(Rhs, K) ((Rhs)[K])
#ifndef YYLLOC_DEFAULT
# define YYLLOC_DEFAULT(Current, Rhs, N)				\
    do									\
      if (YYID (N))                                                    \
	{								\
	  (Current).first_line   = YYRHSLOC (Rhs, 1).first_line;	\
	  (Current).first_column = YYRHSLOC (Rhs, 1).first_column;	\
	  (Current).last_line    = YYRHSLOC (Rhs, N).last_line;		\
	  (Current).last_column  = YYRHSLOC (Rhs, N).last_column;	\
	}								\
      else								\
	{								\
	  (Current).first_line   = (Current).last_line   =		\
	    YYRHSLOC (Rhs, 0).last_line;				\
	  (Current).first_column = (Current).last_column =		\
	    YYRHSLOC (Rhs, 0).last_column;				\
	}								\
    while (YYID (0))
#endif
/* YY_LOCATION_PRINT -- Print the location on the stream.
   This macro was not mandated originally: define only if we know
   we won't break user code: when these are the locations we know.  */
#ifndef YY_LOCATION_PRINT
# if defined YYLTYPE_IS_TRIVIAL && YYLTYPE_IS_TRIVIAL
#  define YY_LOCATION_PRINT(File, Loc)			\
     fprintf (File, "%d.%d-%d.%d",			\
	      (Loc).first_line, (Loc).first_column,	\
	      (Loc).last_line,  (Loc).last_column)
# else
#  define YY_LOCATION_PRINT(File, Loc) ((void) 0)
# endif
#endif
/* YYLEX -- calling `yylex' with the right arguments.  */
#ifdef YYLEX_PARAM
# define YYLEX yylex (&yylval, &yylloc, YYLEX_PARAM)
#else
# define YYLEX yylex (&yylval, &yylloc, scanner)
#endif
# define YYDPRINTF(Args)
# define YY_SYMBOL_PRINT(Title, Type, Value, Location)
# define YY_STACK_PRINT(Bottom, Top)
# define YY_REDUCE_PRINT(Rule)
/* YYINITDEPTH -- initial size of the parser's stacks.  */
#ifndef	YYINITDEPTH
# define YYINITDEPTH 200
#endif
/* YYMAXDEPTH -- maximum size the stacks can grow to (effective only
   if the built-in stack extension method is used).
   Do not make this value too large; the results are undefined if
   YYSTACK_ALLOC_MAXIMUM < YYSTACK_BYTES (YYMAXDEPTH)
   evaluated with infinite-precision integer arithmetic.  */
#ifndef YYMAXDEPTH
# define YYMAXDEPTH 10000
#endif
/*-----------------------------------------------.
| Release the memory associated to this symbol.  |
`-----------------------------------------------*/
/*ARGSUSED*/
#if (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
static void
yydestruct (const char *yymsg, int32_t yytype, YYSTYPE *yyvaluep, YYLTYPE *yylocationp, compileContext* context)
#else
static void
yydestruct (yymsg, yytype, yyvaluep, yylocationp, context)
    const char *yymsg;
    int32_t yytype;
    YYSTYPE *yyvaluep;
    YYLTYPE *yylocationp;
    compileContext* context;
#endif
{
  YYUSE (yyvaluep);
  YYUSE (yylocationp);
  YYUSE (context);
  if (!yymsg)
    yymsg = "Deleting";
  YY_SYMBOL_PRINT (yymsg, yytype, yyvaluep, yylocationp);
  switch (yytype)
    {
      case 3: /* "VALUE" */
#line 8 "eel2.y"
	{
 #define yydestruct(a,b,c,d,e)
};
#line 1222 "y.tab.c"
	break;
      default:
	break;
    }
}
/* Prevent warnings from -Wmissing-prototypes.  */
#ifdef YYPARSE_PARAM
#if defined __STDC__ || defined __cplusplus
int32_t yyparse (void *YYPARSE_PARAM);
#else
int32_t yyparse ();
#endif
#else /* ! YYPARSE_PARAM */
#if defined __STDC__ || defined __cplusplus
int32_t yyparse (compileContext* context);
#else
int32_t yyparse ();
#endif
#endif /* ! YYPARSE_PARAM */
/*----------.
| yyparse.  |
`----------*/
#ifdef YYPARSE_PARAM
#if (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
int32_t
yyparse (void *YYPARSE_PARAM)
#else
int32_t
yyparse (YYPARSE_PARAM)
    void *YYPARSE_PARAM;
#endif
#else /* ! YYPARSE_PARAM */
#if (defined __STDC__ || defined __C99__FUNC__ \
     || defined __cplusplus || defined _MSC_VER)
int32_t
yyparse (compileContext* context)
#else
int32_t
yyparse (context)
    compileContext* context;
#endif
#endif
{
  /* The look-ahead symbol.  */
int32_t yychar;
/* The semantic value of the look-ahead symbol.  */
YYSTYPE yylval = 0;
/* Number of syntax errors so far.  */
int32_t yynerrs;
/* Location data for the look-ahead symbol.  */
YYLTYPE yylloc;
  int32_t yystate;
  int32_t yyn;
  int32_t yyresult;
  /* Number of tokens to shift before error messages enabled.  */
  int32_t yyerrstatus;
  /* Look-ahead token as an internal (translated) token number.  */
  int32_t yytoken = 0;
#if YYERROR_VERBOSE
  /* Buffer for error messages, and its allocated size.  */
  char yymsgbuf[128];
  char *yymsg = yymsgbuf;
  YYSIZE_T yymsg_alloc = sizeof yymsgbuf;
#endif
  /* Three stacks and their tools:
     `yyss': related to states,
     `yyvs': related to semantic values,
     `yyls': related to locations.
     Refer to the stacks thru separate pointers, to allow yyoverflow
     to reallocate them elsewhere.  */
  /* The state stack.  */
  int16_t yyssa[YYINITDEPTH];
  int16_t *yyss = yyssa;
  int16_t *yyssp;
  /* The semantic value stack.  */
  YYSTYPE yyvsa[YYINITDEPTH];
  YYSTYPE *yyvs = yyvsa;
  YYSTYPE *yyvsp;
  /* The location stack.  */
  YYLTYPE yylsa[YYINITDEPTH];
  YYLTYPE *yyls = yylsa;
  YYLTYPE *yylsp;
  /* The locations where the error started and ended.  */
  YYLTYPE yyerror_range[2];
#define YYPOPSTACK(N)   (yyvsp -= (N), yyssp -= (N), yylsp -= (N))
  YYSIZE_T yystacksize = YYINITDEPTH;
  /* The variables used to return semantic value and location from the
     action routines.  */
  YYSTYPE yyval;
  YYLTYPE yyloc;
  /* The number of symbols on the RHS of the reduced rule.
     Keep to zero when no symbol should be popped.  */
  int32_t yylen = 0;
  YYDPRINTF ((stderr, "Starting parse\n"));
  yystate = 0;
  yyerrstatus = 0;
  yynerrs = 0;
  yychar = YYEMPTY;		/* Cause a token to be read.  */
  /* Initialize stack pointers.
     Waste one element of value and location stack
     so that they stay on the same level as the state stack.
     The wasted elements are never initialized.  */
  yyssp = yyss;
  yyvsp = yyvs;
  yylsp = yyls;
#if defined YYLTYPE_IS_TRIVIAL && YYLTYPE_IS_TRIVIAL
  /* Initialize the default location before parsing starts.  */
  yylloc.first_line   = yylloc.last_line   = 1;
  yylloc.first_column = yylloc.last_column = 0;
#endif
  goto yysetstate;
/*------------------------------------------------------------.
| yynewstate -- Push a new state, which is found in yystate.  |
`------------------------------------------------------------*/
 yynewstate:
  /* In all cases, when you get here, the value and location stacks
     have just been pushed.  So pushing a state here evens the stacks.  */
  yyssp++;
 yysetstate:
  *yyssp = yystate;
  if (yyss + yystacksize - 1 <= yyssp)
    {
      /* Get the current used size of the three stacks, in elements.  */
      YYSIZE_T yysize = yyssp - yyss + 1;
#ifdef yyoverflow
      {
	/* Give user a chance to reallocate the stack.  Use copies of
	   these so that the &'s don't force the real ones into
	   memory.  */
	YYSTYPE *yyvs1 = yyvs;
	int16_t *yyss1 = yyss;
	YYLTYPE *yyls1 = yyls;
	/* Each stack pointer address is followed by the size of the
	   data in use in that stack, in bytes.  This used to be a
	   conditional around just the two extra args, but that might
	   be undefined if yyoverflow is a macro.  */
	yyoverflow (YY_("memory exhausted"),
		    &yyss1, yysize * sizeof (*yyssp),
		    &yyvs1, yysize * sizeof (*yyvsp),
		    &yyls1, yysize * sizeof (*yylsp),
		    &yystacksize);
	yyls = yyls1;
	yyss = yyss1;
	yyvs = yyvs1;
      }
#else /* no yyoverflow */
# ifndef YYSTACK_RELOCATE
      goto yyexhaustedlab;
# else
      /* Extend the stack our own way.  */
      if (YYMAXDEPTH <= yystacksize)
	goto yyexhaustedlab;
      yystacksize *= 2;
      if (YYMAXDEPTH < yystacksize)
	yystacksize = YYMAXDEPTH;
      {
	int16_t *yyss1 = yyss;
	union yyalloc *yyptr =
	  (union yyalloc *) YYSTACK_ALLOC (YYSTACK_BYTES (yystacksize));
	if (! yyptr)
	  goto yyexhaustedlab;
	YYSTACK_RELOCATE (yyss);
	YYSTACK_RELOCATE (yyvs);
	YYSTACK_RELOCATE (yyls);
#  undef YYSTACK_RELOCATE
	if (yyss1 != yyssa)
	  YYSTACK_FREE (yyss1);
      }
# endif
#endif /* no yyoverflow */
      yyssp = yyss + yysize - 1;
      yyvsp = yyvs + yysize - 1;
      yylsp = yyls + yysize - 1;
      YYDPRINTF ((stderr, "Stack size increased to %lu\n",
		  (unsigned long int32_t) yystacksize));
      if (yyss + yystacksize - 1 <= yyssp)
	YYABORT;
    }
  YYDPRINTF ((stderr, "Entering state %d\n", yystate));
  goto yybackup;
/*-----------.
| yybackup.  |
`-----------*/
yybackup:
  /* Do appropriate processing given the current state.  Read a
     look-ahead token if we need one and don't already have one.  */
  /* First try to decide what to do without reference to look-ahead token.  */
  yyn = yypact[yystate];
  if (yyn == YYPACT_NINF)
    goto yydefault;
  /* Not known => get a look-ahead token if don't already have one.  */
  /* YYCHAR is either YYEMPTY or YYEOF or a valid look-ahead symbol.  */
  if (yychar == YYEMPTY)
    {
      YYDPRINTF ((stderr, "Reading a token: "));
      yychar = YYLEX;
    }
  if (yychar <= YYEOF)
    {
      yychar = yytoken = YYEOF;
      YYDPRINTF ((stderr, "Now at end of input.\n"));
    }
  else
    {
      yytoken = YYTRANSLATE (yychar);
      YY_SYMBOL_PRINT ("Next token is", yytoken, &yylval, &yylloc);
    }
  /* If the proper action on seeing token YYTOKEN is to reduce or to
     detect an error, take that action.  */
  yyn += yytoken;
  if (yyn < 0 || YYLAST < yyn || yycheck[yyn] != yytoken)
    goto yydefault;
  yyn = yytable[yyn];
  if (yyn <= 0)
    {
      if (yyn == 0 || yyn == YYTABLE_NINF)
	goto yyerrlab;
      yyn = -yyn;
      goto yyreduce;
    }
  if (yyn == YYFINAL)
    YYACCEPT;
  /* Count tokens shifted since error; after three, turn off error
     status.  */
  if (yyerrstatus)
    yyerrstatus--;
  /* Shift the look-ahead token.  */
  YY_SYMBOL_PRINT ("Shifting", yytoken, &yylval, &yylloc);
  /* Discard the shifted token unless it is eof.  */
  if (yychar != YYEOF)
    yychar = YYEMPTY;
  yystate = yyn;
  *++yyvsp = yylval;
  *++yylsp = yylloc;
  goto yynewstate;
/*-----------------------------------------------------------.
| yydefault -- do the default action for the current state.  |
`-----------------------------------------------------------*/
yydefault:
  yyn = yydefact[yystate];
  if (yyn == 0)
    goto yyerrlab;
  goto yyreduce;
/*-----------------------------.
| yyreduce -- Do a reduction.  |
`-----------------------------*/
yyreduce:
  /* yyn is the number of a rule to reduce with.  */
  yylen = yyr2[yyn];
  /* If YYLEN is nonzero, implement the default value of the action:
     `$$ = $1'.
     Otherwise, the following line sets YYVAL to garbage.
     This behavior is undocumented and Bison
     users should not rely upon it.  Assigning to YYVAL
     unconditionally makes the parser a bit smaller, and it avoids a
     GCC warning that YYVAL may be used uninitialized.  */
  yyval = yyvsp[1-yylen];
  /* Default location.  */
  YYLLOC_DEFAULT (yyloc, (yylsp - yylen), yylen);
  YY_REDUCE_PRINT (yyn);
  switch (yyn)
    {
        case 3:
#line 42 "eel2.y"
    {
	  (yyval) = nseel_createMoreParametersOpcode(context,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
	}
    break;
  case 5:
#line 50 "eel2.y"
    {
          ((eelStringSegmentRec *)(yyvsp[(1) - (2)]))->_next = (eelStringSegmentRec *)(yyvsp[(2) - (2)]);
          (yyval) = (yyvsp[(1) - (2)]);
        }
    break;
  case 6:
#line 58 "eel2.y"
    {
          if (!((yyval) = nseel_resolve_named_symbol(context, (yyvsp[(1) - (1)]), -1, NULL))) /* convert from purely named to namespace-relative, etc */
          {
            yyerror(&yyloc, context, ""); 
            YYERROR;
          }
        }
    break;
  case 7:
#line 68 "eel2.y"
    {
	  (yyval) = (yyvsp[(2) - (3)]);
	}
    break;
  case 8:
#line 72 "eel2.y"
    {
          int32_t err;
  	  if (!((yyval) = nseel_setCompiledFunctionCallParameters(context,(yyvsp[(1) - (7)]), (yyvsp[(3) - (7)]), 0, 0, (yyvsp[(6) - (7)]), &err))) 
          { 
            if (err == -1) yyerror(&yylsp[-2], context, "");
            else if (err == 0) yyerror(&yylsp[-6], context, "");
            else yyerror(&yylsp[-3], context, ""); // parameter count wrong
            YYERROR; 
          }
	}
    break;
  case 9:
#line 84 "eel2.y"
    {
          int32_t err;
  	  if (!((yyval) = nseel_setCompiledFunctionCallParameters(context,(yyvsp[(1) - (4)]), (yyvsp[(3) - (4)]), 0, 0, 0, &err))) 
          { 
            if (err == 0) yyerror(&yylsp[-3], context, "");
            else yyerror(&yylsp[0], context, ""); // parameter count wrong
            YYERROR; 
          }
	}
    break;
  case 10:
#line 94 "eel2.y"
    {
          int32_t err;
  	  if (!((yyval) = nseel_setCompiledFunctionCallParameters(context,(yyvsp[(1) - (3)]), nseel_createCompiledValue(context,0.0), 0, 0, 0,&err))) 
          { 
            if (err == 0) yyerror(&yylsp[-2], context, ""); // function not found
            else yyerror(&yylsp[0], context, ""); // parameter count wrong
            YYERROR; 
          }
	}
    break;
  case 11:
#line 104 "eel2.y"
    {
          int32_t err;
  	  if (!((yyval) = nseel_setCompiledFunctionCallParameters(context,(yyvsp[(1) - (6)]), (yyvsp[(3) - (6)]), (yyvsp[(5) - (6)]), 0, 0,&err))) 
          { 
            if (err == 0) yyerror(&yylsp[-5], context, "");
            else if (err == 2) yyerror(&yylsp[0], context, ""); // needs more than 2 parameters
            else yyerror(&yylsp[-2], context, ""); // less than 2
            YYERROR; 
          }
	}
    break;
  case 12:
#line 115 "eel2.y"
    {
          int32_t err;
  	  if (!((yyval) = nseel_setCompiledFunctionCallParameters(context,(yyvsp[(1) - (8)]), (yyvsp[(3) - (8)]), (yyvsp[(5) - (8)]), (yyvsp[(7) - (8)]), 0, &err))) 
          { 
            if (err == 0) yyerror(&yylsp[-7], context, "");
            else if (err==2) yyerror(&yylsp[0], context, ""); // needs more parameters
            else if (err==4) yyerror(&yylsp[-4], context, ""); // needs single parameter
            else yyerror(&yylsp[-2], context, ""); // less parm
            YYERROR; 
          }
	}
    break;
  case 13:
#line 127 "eel2.y"
    {
	  (yyval) = nseel_createMemoryAccess(context,(yyvsp[(1) - (3)]),0);
        }
    break;
  case 14:
#line 131 "eel2.y"
    {
	  (yyval) = nseel_createMemoryAccess(context,(yyvsp[(1) - (4)]),(yyvsp[(3) - (4)]));
        }
    break;
  case 17:
#line 140 "eel2.y"
    {
          (yyval) = nseel_eelMakeOpcodeFromStringSegments(context,(eelStringSegmentRec *)(yyvsp[(1) - (1)]));
        }
    break;
  case 20:
#line 150 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_ASSIGN,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 21:
#line 154 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_ADD_OP,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 22:
#line 158 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_SUB_OP,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 23:
#line 162 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_MOD_OP,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 24:
#line 166 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_OR_OP,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 25:
#line 170 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_AND_OP,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 26:
#line 174 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_XOR_OP,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 27:
#line 178 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_DIV_OP,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 28:
#line 182 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_MUL_OP,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 29:
#line 186 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_POW_OP,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 30:
#line 190 "eel2.y"
    {
          (yyval) = nseel_createFunctionByName(context,"strcpy",2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]),NULL); 
        }
    break;
  case 31:
#line 194 "eel2.y"
    {
          (yyval) = nseel_createFunctionByName(context,"strcat",2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]),NULL); 
        }
    break;
  case 33:
#line 202 "eel2.y"
    {
	  (yyval) = (yyvsp[(2) - (2)]);
	}
    break;
  case 34:
#line 206 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_UMINUS,1,(yyvsp[(2) - (2)]),0);
	}
    break;
  case 35:
#line 210 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_NOT,1,(yyvsp[(2) - (2)]),0);
	}
    break;
  case 37:
#line 218 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_POW,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 39:
#line 226 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_MOD,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 40:
#line 230 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_SHL,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 41:
#line 234 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_SHR,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 43:
#line 242 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_DIVIDE,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
	}
    break;
  case 45:
#line 251 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_MULTIPLY,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
	}
    break;
  case 47:
#line 260 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_SUB,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
	}
    break;
  case 49:
#line 268 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_ADD,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
	}
    break;
  case 51:
#line 276 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_AND,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
	}
    break;
  case 52:
#line 280 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_OR,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
	}
    break;
  case 53:
#line 284 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_XOR,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
	}
    break;
  case 55:
#line 292 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_LT,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 56:
#line 296 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_GT,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 57:
#line 300 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_LTE,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 58:
#line 304 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_GTE,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 59:
#line 308 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_EQ,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 60:
#line 312 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_EQ_EXACT,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 61:
#line 316 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_NE,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 62:
#line 320 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_NE_EXACT,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 64:
#line 328 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_LOGICAL_AND,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 65:
#line 332 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_LOGICAL_OR,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
        }
    break;
  case 67:
#line 340 "eel2.y"
    {
	  (yyval) = nseel_createIfElse(context, (yyvsp[(1) - (5)]), (yyvsp[(3) - (5)]), (yyvsp[(5) - (5)]));
        }
    break;
  case 68:
#line 344 "eel2.y"
    {
	  (yyval) = nseel_createIfElse(context, (yyvsp[(1) - (4)]), 0, (yyvsp[(4) - (4)]));
        }
    break;
  case 69:
#line 348 "eel2.y"
    {
	  (yyval) = nseel_createIfElse(context, (yyvsp[(1) - (3)]), (yyvsp[(3) - (3)]), 0);
        }
    break;
  case 71:
#line 357 "eel2.y"
    {
	  (yyval) = nseel_createSimpleCompiledFunction(context,FN_JOIN_STATEMENTS,2,(yyvsp[(1) - (3)]),(yyvsp[(3) - (3)]));
	}
    break;
  case 72:
#line 361 "eel2.y"
    {
	  (yyval) = (yyvsp[(1) - (2)]);
	}
    break;
  case 73:
#line 369 "eel2.y"
    { 
                if ((yylsp[(1) - (1)]).first_line) { }
                context->result = (yyvsp[(1) - (1)]);
	}
    break;
/* Line 1267 of yacc.c.  */
#line 1965 "y.tab.c"
      default: break;
    }
  YY_SYMBOL_PRINT ("-> $$ =", yyr1[yyn], &yyval, &yyloc);
  YYPOPSTACK (yylen);
  yylen = 0;
  YY_STACK_PRINT (yyss, yyssp);
  *++yyvsp = yyval;
  *++yylsp = yyloc;
  /* Now `shift' the result of the reduction.  Determine what state
     that goes to, based on the state we popped back to and the rule
     number reduced by.  */
  yyn = yyr1[yyn];
  yystate = yypgoto[yyn - YYNTOKENS] + *yyssp;
  if (0 <= yystate && yystate <= YYLAST && yycheck[yystate] == *yyssp)
    yystate = yytable[yystate];
  else
    yystate = yydefgoto[yyn - YYNTOKENS];
  goto yynewstate;
/*------------------------------------.
| yyerrlab -- here on detecting error |
`------------------------------------*/
yyerrlab:
  /* If not already recovering from an error, report this error.  */
  if (!yyerrstatus)
    {
      ++yynerrs;
	  yyerror(&yylloc, context, YY_("syntax error"));
    }
  yyerror_range[0] = yylloc;
  if (yyerrstatus == 3)
    {
      /* If just tried and failed to reuse look-ahead token after an
	 error, discard it.  */
      if (yychar <= YYEOF)
	{
	  /* Return failure if at end of input.  */
	  if (yychar == YYEOF)
	    YYABORT;
	}
      else
	{
	  yydestruct ("Error: discarding",
		      yytoken, &yylval, &yylloc, context);
	  yychar = YYEMPTY;
	}
    }
  /* Else will try to reuse look-ahead token after shifting the error
     token.  */
  goto yyerrlab1;
/*---------------------------------------------------.
| yyerrorlab -- error raised explicitly by YYERROR.  |
`---------------------------------------------------*/
yyerrorlab:
  /* Pacify compilers like GCC when the user code never invokes
     YYERROR and the label yyerrorlab therefore never appears in user
     code.  */
  yyerror_range[0] = yylsp[1-yylen];
  /* Do not reclaim the symbols of the rule which action triggered
     this YYERROR.  */
  YYPOPSTACK (yylen);
  yylen = 0;
  YY_STACK_PRINT (yyss, yyssp);
  yystate = *yyssp;
  goto yyerrlab1;
/*-------------------------------------------------------------.
| yyerrlab1 -- common code for both syntax error and YYERROR.  |
`-------------------------------------------------------------*/
yyerrlab1:
  yyerrstatus = 3;	/* Each real token shifted decrements this.  */
  for (;;)
    {
      yyn = yypact[yystate];
      if (yyn != YYPACT_NINF)
	{
	  yyn += YYTERROR;
	  if (0 <= yyn && yyn <= YYLAST && yycheck[yyn] == YYTERROR)
	    {
	      yyn = yytable[yyn];
	      if (0 < yyn)
		break;
	    }
	}
      /* Pop the current state because it cannot handle the error token.  */
      if (yyssp == yyss)
	YYABORT;
      yyerror_range[0] = *yylsp;
      yydestruct ("Error: popping",
		  yystos[yystate], yyvsp, yylsp, context);
      YYPOPSTACK (1);
      yystate = *yyssp;
      YY_STACK_PRINT (yyss, yyssp);
    }
  if (yyn == YYFINAL)
    YYACCEPT;
  *++yyvsp = yylval;
  yyerror_range[1] = yylloc;
  /* Using YYLLOC is tempting, but would change the location of
     the look-ahead.  YYLOC is available though.  */
  YYLLOC_DEFAULT (yyloc, (yyerror_range - 1), 2);
  *++yylsp = yyloc;
  /* Shift the error token.  */
  YY_SYMBOL_PRINT ("Shifting", yystos[yyn], yyvsp, yylsp);
  yystate = yyn;
  goto yynewstate;
/*-------------------------------------.
| yyacceptlab -- YYACCEPT comes here.  |
`-------------------------------------*/
yyacceptlab:
  yyresult = 0;
  goto yyreturn;
/*-----------------------------------.
| yyabortlab -- YYABORT comes here.  |
`-----------------------------------*/
yyabortlab:
  yyresult = 1;
  goto yyreturn;
#ifndef yyoverflow
/*-------------------------------------------------.
| yyexhaustedlab -- memory exhaustion comes here.  |
`-------------------------------------------------*/
yyexhaustedlab:
  yyerror (&yylloc, context, YY_("memory exhausted"));
  yyresult = 2;
  /* Fall through.  */
#endif
yyreturn:
  if (yychar != YYEOF && yychar != YYEMPTY)
     yydestruct ("Cleanup: discarding lookahead",
		 yytoken, &yylval, &yylloc, context);
  /* Do not reclaim the symbols of the rule which action triggered
     this YYABORT or YYACCEPT.  */
  YYPOPSTACK (yylen);
  YY_STACK_PRINT (yyss, yyssp);
  while (yyssp != yyss)
    {
      yydestruct ("Cleanup: popping",
		  yystos[*yyssp], yyvsp, yylsp, context);
      YYPOPSTACK (1);
    }
#ifndef yyoverflow
  if (yyss != yyssa)
    YYSTACK_FREE (yyss);
#endif
#if YYERROR_VERBOSE
  if (yymsg != yymsgbuf)
    YYSTACK_FREE (yymsg);
#endif
  /* Make sure YYID is used.  */
  return YYID (yyresult);
}