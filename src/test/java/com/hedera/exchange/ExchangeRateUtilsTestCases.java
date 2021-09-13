package com.hedera.exchange;

/*-
 * ‌
 * Hedera Exchange Rate Tool
 * ​
 * Copyright (C) 2019 - 2020 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 *
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * * Neither the name of JSR-310 nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.hedera.exchange.exchanges.Exchange;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.proto.NodeAddressBook;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ExchangeRateUtilsTestCases {

	private Map<String, String> nodes =  new HashMap<>();
	private NodeAddressBook addressBook;

	@Test
	public void verifyNodesFromAddressBook() throws IOException {
		setupAddressBook();

		Map<String, String> ERTnodes = ExchangeRateUtils.getNodesFromAddressBook(addressBook);
		for( String node : ERTnodes.keySet()){
			assertEquals(ERTnodes.get(node), nodes.get(node));
		}
	}

	@Test
	public void getNodesForClientTest() throws IOException {
		//setup
		setupAddressBook();

		//when
		Map<String, AccountId> nodesForClient = ExchangeRateUtils.getNodesForClient(nodes);

		//then
		assertEquals(13, nodesForClient.size());
		assertEquals(3, nodesForClient.get("35.237.182.66:50211").num);
		assertEquals(7, nodesForClient.get("34.94.236.63:50211").num);
		assertEquals(11, nodesForClient.get("35.246.250.176:50211").num);
		assertEquals(15, nodesForClient.get("34.87.47.168:50211").num);
	}

	@Test
	@Disabled
	public void getDecryptedEnvironmentVariableFromAWSTest() {
		final String expectedValue = "https://s3.amazonaws.com/exchange.rate.config.integration/config.json";
		final String encryptedValue = "AQICAHi3BYYdRzjj1ZR5ij/3mN6+GWqEbw7NTAG0fm7nzYo3MwHyBlKsmA+1lepLUe" +
				"+0rgeFAAAApzCBpAYJKoZIhvcNAQcGoIGWMIGTAgEAMIGNBgkqhkiG9w0BBwEwHgYJYIZIAWUDBAEuMBEEDLHJjkIANloMVIhCdgIBEIBg9HnXBKnxE3c4H5/17ilQR0G6DqZKH6dzBnhkUAjYbg1sBuStjVA8rQwBUtiSKO7b5ehQh+OxnrJxVbHAZNylSH71fr7OICMI3iA2qkIM8gtWNG1htphGhkDLCRcaw5Xh";
		final String lambdaFunctioName = "exchange-rate-tool-lambda-integration";
		assertEquals(expectedValue, ExchangeRateUtils.getDecryptedValueFromAWS(encryptedValue, lambdaFunctioName));
	}

	@Test
	public void generateExchangesTest() {
		//setup
		Map<String, String> exchangeAPIs = new HashMap<String, String>() {{
			put("bitrex", "https://api.bittrex.com/api/v1.1/public/getmarketsummary?market=USD-HBAR");
			put("okcoin", "https://www.okcoin.com/api/spot/v3/instruments/HBAR-USD/ticker");
			put("paybito", "https://trade.paybito.com/api/trades/HBAR_USD");
		}};

		//when
		List<Exchange> exchanges = ExchangeRateUtils.generateExchanges(exchangeAPIs);

		//then
		assertEquals(3, exchanges.size());
		assertNotNull(exchanges.get(0).getHBarValue());
		assertNotNull(exchanges.get(1).getHBarValue());
		assertNotNull(exchanges.get(2).getHBarValue());
	}

	@Test
	public void testcalculateNewExchangeRate() throws IOException {
		//given
		Rate activeRate = new Rate(30000 ,960000, 123456789L);
		ExchangeRate exchangeRate = new ExchangeRate(
				new Rate(30000 ,450000, 123456789L),
				new Rate(30000 ,510000, 123456789L));

		//when
		ExchangeRate newExchangeRate = ExchangeRateUtils.calculateNewExchangeRate(activeRate, exchangeRate);

		//then
		assertEquals(501000, newExchangeRate.getCurrentRate().getCentEquiv());
		assertEquals(555000, newExchangeRate.getNextRate().getCentEquiv());

		newExchangeRate = ExchangeRateUtils.calculateNewExchangeRate(activeRate, newExchangeRate);

		assertEquals(528000, newExchangeRate.getCurrentRate().getCentEquiv());
		assertEquals(582000, newExchangeRate.getNextRate().getCentEquiv());
	}

	@Test
	public void test() {
		String[] temp = new String[] {
				"abandon", "ability", "able", "about", "above", "absent", "absorb", "abstract", "absurd",
				"abuse", "access", "accident", "account", "accuse", "achieve", "acid", "acoustic", "acquire",
				"across", "act", "action", "actor", "actress", "actual", "adapt", "add", "addict", "address",
				"adjust", "admit", "adult", "advance", "advice", "aerobic", "affair", "afford", "afraid",
				"again", "age", "agent", "agree", "ahead", "aim", "air", "airport", "aisle", "alarm", "album",
				"alcohol", "alert", "alien", "all", "alley", "allow", "almost", "alone", "alpha", "already",
				"also", "alter", "always", "amateur", "amazing", "among", "amount", "amused", "analyst",
				"anchor", "ancient", "anger", "angle", "angry", "animal", "ankle", "announce", "annual",
				"another", "answer", "antenna", "antique", "anxiety", "any", "apart", "apology", "appear",
				"apple", "approve", "april", "arch", "arctic", "area", "arena", "argue", "arm", "armed",
				"armor", "army", "around", "arrange", "arrest", "arrive", "arrow", "art", "artefact", "artist",
				"artwork", "ask", "aspect", "assault", "asset", "assist", "assume", "asthma", "athlete",
				"atom", "attack", "attend", "attitude", "attract", "auction", "audit", "august", "aunt",
				"author", "auto", "autumn", "average", "avocado", "avoid", "awake", "aware", "away", "awesome",
				"awful", "awkward", "axis", "baby", "bachelor", "bacon", "badge", "bag", "balance", "balcony",
				"ball", "bamboo", "banana", "banner", "bar", "barely", "bargain", "barrel", "base", "basic",
				"basket", "battle", "beach", "bean", "beauty", "because", "become", "beef", "before", "begin",
				"behave", "behind", "believe", "below", "belt", "bench", "benefit", "best", "betray", "better",
				"between", "beyond", "bicycle", "bid", "bike", "bind", "biology", "bird", "birth", "bitter",
				"black", "blade", "blame", "blanket", "blast", "bleak", "bless", "blind", "blood", "blossom",
				"blouse", "blue", "blur", "blush", "board", "boat", "body", "boil", "bomb", "bone", "bonus",
				"book", "boost", "border", "boring", "borrow", "boss", "bottom", "bounce", "box", "boy",
				"bracket", "brain", "brand", "brass", "brave", "bread", "breeze", "brick", "bridge", "brief",
				"bright", "bring", "brisk", "broccoli", "broken", "bronze", "broom", "brother", "brown",
				"brush", "bubble", "buddy", "budget", "buffalo", "build", "bulb", "bulk", "bullet", "bundle",
				"bunker", "burden", "burger", "burst", "bus", "business", "busy", "butter", "buyer", "buzz",
				"cabbage", "cabin", "cable", "cactus", "cage", "cake", "call", "calm", "camera", "camp", "can",
				"canal", "cancel", "candy", "cannon", "canoe", "canvas", "canyon", "capable", "capital",
				"captain", "car", "carbon", "card", "cargo", "carpet", "carry", "cart", "case", "cash",
				"casino", "castle", "casual", "cat", "catalog", "catch", "category", "cattle", "caught",
				"cause", "caution", "cave", "ceiling", "celery", "cement", "census", "century", "cereal",
				"certain", "chair", "chalk", "champion", "change", "chaos", "chapter", "charge", "chase",
				"chat", "cheap", "check", "cheese", "chef", "cherry", "chest", "chicken", "chief", "child",
				"chimney", "choice", "choose", "chronic", "chuckle", "chunk", "churn", "cigar", "cinnamon",
				"circle", "citizen", "city", "civil", "claim", "clap", "clarify", "claw", "clay", "clean",
				"clerk", "clever", "click", "client", "cliff", "climb", "clinic", "clip", "clock", "clog",
				"close", "cloth", "cloud", "clown", "club", "clump", "cluster", "clutch", "coach", "coast",
				"coconut", "code", "coffee", "coil", "coin", "collect", "color", "column", "combine", "come",
				"comfort", "comic", "common", "company", "concert", "conduct", "confirm", "congress",
				"connect", "consider", "control", "convince", "cook", "cool", "copper", "copy", "coral",
				"core", "corn", "correct", "cost", "cotton", "couch", "country", "couple", "course", "cousin",
				"cover", "coyote", "crack", "cradle", "craft", "cram", "crane", "crash", "crater", "crawl",
				"crazy", "cream", "credit", "creek", "crew", "cricket", "crime", "crisp", "critic", "crop",
				"cross", "crouch", "crowd", "crucial", "cruel", "cruise", "crumble", "crunch", "crush", "cry",
				"crystal", "cube", "culture", "cup", "cupboard", "curious", "current", "curtain", "curve",
				"cushion", "custom", "cute", "cycle", "dad", "damage", "damp", "dance", "danger", "daring",
				"dash", "daughter", "dawn", "day", "deal", "debate", "debris", "decade", "december", "decide",
				"decline", "decorate", "decrease", "deer", "defense", "define", "defy", "degree", "delay",
				"deliver", "demand", "demise", "denial", "dentist", "deny", "depart", "depend", "deposit",
				"depth", "deputy", "derive", "describe", "desert", "design", "desk", "despair", "destroy",
				"detail", "detect", "develop", "device", "devote", "diagram", "dial", "diamond", "diary",
				"dice", "diesel", "diet", "differ", "digital", "dignity", "dilemma", "dinner", "dinosaur",
				"direct", "dirt", "disagree", "discover", "disease", "dish", "dismiss", "disorder", "display",
				"distance", "divert", "divide", "divorce", "dizzy", "doctor", "document", "dog", "doll",
				"dolphin", "domain", "donate", "donkey", "donor", "door", "dose", "double", "dove", "draft",
				"dragon", "drama", "drastic", "draw", "dream", "dress", "drift", "drill", "drink", "drip",
				"drive", "drop", "drum", "dry", "duck", "dumb", "dune", "during", "dust", "dutch", "duty",
				"dwarf", "dynamic", "eager", "eagle", "early", "earn", "earth", "easily", "east", "easy",
				"echo", "ecology", "economy", "edge", "edit", "educate", "effort", "egg", "eight", "either",
				"elbow", "elder", "electric", "elegant", "element", "elephant", "elevator", "elite", "else",
				"embark", "embody", "embrace", "emerge", "emotion", "employ", "empower", "empty", "enable",
				"enact", "end", "endless", "endorse", "enemy", "energy", "enforce", "engage", "engine",
				"enhance", "enjoy", "enlist", "enough", "enrich", "enroll", "ensure", "enter", "entire",
				"entry", "envelope", "episode", "equal", "equip", "era", "erase", "erode", "erosion", "error",
				"erupt", "escape", "essay", "essence", "estate", "eternal", "ethics", "evidence", "evil",
				"evoke", "evolve", "exact", "example", "excess", "exchange", "excite", "exclude", "excuse",
				"execute", "exercise", "exhaust", "exhibit", "exile", "exist", "exit", "exotic", "expand",
				"expect", "expire", "explain", "expose", "express", "extend", "extra", "eye", "eyebrow",
				"fabric", "face", "faculty", "fade", "faint", "faith", "fall", "false", "fame", "family",
				"famous", "fan", "fancy", "fantasy", "farm", "fashion", "fat", "fatal", "father", "fatigue",
				"fault", "favorite", "feature", "february", "federal", "fee", "feed", "feel", "female",
				"fence", "festival", "fetch", "fever", "few", "fiber", "fiction", "field", "figure", "file",
				"film", "filter", "final", "find", "fine", "finger", "finish", "fire", "firm", "first",
				"fiscal", "fish", "fit", "fitness", "fix", "flag", "flame", "flash", "flat", "flavor", "flee",
				"flight", "flip", "float", "flock", "floor", "flower", "fluid", "flush", "fly", "foam",
				"focus", "fog", "foil", "fold", "follow", "food", "foot", "force", "forest", "forget", "fork",
				"fortune", "forum", "forward", "fossil", "foster", "found", "fox", "fragile", "frame",
				"frequent", "fresh", "friend", "fringe", "frog", "front", "frost", "frown", "frozen", "fruit",
				"fuel", "fun", "funny", "furnace", "fury", "future", "gadget", "gain", "galaxy", "gallery",
				"game", "gap", "garage", "garbage", "garden", "garlic", "garment", "gas", "gasp", "gate",
				"gather", "gauge", "gaze", "general", "genius", "genre", "gentle", "genuine", "gesture",
				"ghost", "giant", "gift", "giggle", "ginger", "giraffe", "girl", "give", "glad", "glance",
				"glare", "glass", "glide", "glimpse", "globe", "gloom", "glory", "glove", "glow", "glue",
				"goat", "goddess", "gold", "good", "goose", "gorilla", "gospel", "gossip", "govern", "gown",
				"grab", "grace", "grain", "grant", "grape", "grass", "gravity", "great", "green", "grid",
				"grief", "grit", "grocery", "group", "grow", "grunt", "guard", "guess", "guide", "guilt",
				"guitar", "gun", "gym", "habit", "hair", "half", "hammer", "hamster", "hand", "happy",
				"harbor", "hard", "harsh", "harvest", "hat", "have", "hawk", "hazard", "head", "health",
				"heart", "heavy", "hedgehog", "height", "hello", "helmet", "help", "hen", "hero", "hidden",
				"high", "hill", "hint", "hip", "hire", "history", "hobby", "hockey", "hold", "hole", "holiday",
				"hollow", "home", "honey", "hood", "hope", "horn", "horror", "horse", "hospital", "host",
				"hotel", "hour", "hover", "hub", "huge", "human", "humble", "humor", "hundred", "hungry",
				"hunt", "hurdle", "hurry", "hurt", "husband", "hybrid", "ice", "icon", "idea", "identify",
				"idle", "ignore", "ill", "illegal", "illness", "image", "imitate", "immense", "immune",
				"impact", "impose", "improve", "impulse", "inch", "include", "income", "increase", "index",
				"indicate", "indoor", "industry", "infant", "inflict", "inform", "inhale", "inherit",
				"initial", "inject", "injury", "inmate", "inner", "innocent", "input", "inquiry", "insane",
				"insect", "inside", "inspire", "install", "intact", "interest", "into", "invest", "invite",
				"involve", "iron", "island", "isolate", "issue", "item", "ivory", "jacket", "jaguar", "jar",
				"jazz", "jealous", "jeans", "jelly", "jewel", "job", "join", "joke", "journey", "joy", "judge",
				"juice", "jump", "jungle", "junior", "junk", "just", "kangaroo", "keen", "keep", "ketchup",
				"key", "kick", "kid", "kidney", "kind", "kingdom", "kiss", "kit", "kitchen", "kite", "kitten",
				"kiwi", "knee", "knife", "knock", "know", "lab", "label", "labor", "ladder", "lady", "lake",
				"lamp", "language", "laptop", "large", "later", "latin", "laugh", "laundry", "lava", "law",
				"lawn", "lawsuit", "layer", "lazy", "leader", "leaf", "learn", "leave", "lecture", "left",
				"leg", "legal", "legend", "leisure", "lemon", "lend", "length", "lens", "leopard", "lesson",
				"letter", "level", "liar", "liberty", "library", "license", "life", "lift", "light", "like",
				"limb", "limit", "link", "lion", "liquid", "list", "little", "live", "lizard", "load", "loan",
				"lobster", "local", "lock", "logic", "lonely", "long", "loop", "lottery", "loud", "lounge",
				"love", "loyal", "lucky", "luggage", "lumber", "lunar", "lunch", "luxury", "lyrics", "machine",
				"mad", "magic", "magnet", "maid", "mail", "main", "major", "make", "mammal", "man", "manage",
				"mandate", "mango", "mansion", "manual", "maple", "marble", "march", "margin", "marine",
				"market", "marriage", "mask", "mass", "master", "match", "material", "math", "matrix",
				"matter", "maximum", "maze", "meadow", "mean", "measure", "meat", "mechanic", "medal", "media",
				"melody", "melt", "member", "memory", "mention", "menu", "mercy", "merge", "merit", "merry",
				"mesh", "message", "metal", "method", "middle", "midnight", "milk", "million", "mimic", "mind",
				"minimum", "minor", "minute", "miracle", "mirror", "misery", "miss", "mistake", "mix", "mixed",
				"mixture", "mobile", "model", "modify", "mom", "moment", "monitor", "monkey", "monster",
				"month", "moon", "moral", "more", "morning", "mosquito", "mother", "motion", "motor",
				"mountain", "mouse", "move", "movie", "much", "muffin", "mule", "multiply", "muscle", "museum",
				"mushroom", "music", "must", "mutual", "myself", "mystery", "myth", "naive", "name", "napkin",
				"narrow", "nasty", "nation", "nature", "near", "neck", "need", "negative", "neglect",
				"neither", "nephew", "nerve", "nest", "net", "network", "neutral", "never", "news", "next",
				"nice", "night", "noble", "noise", "nominee", "noodle", "normal", "north", "nose", "notable",
				"note", "nothing", "notice", "novel", "now", "nuclear", "number", "nurse", "nut", "oak",
				"obey", "object", "oblige", "obscure", "observe", "obtain", "obvious", "occur", "ocean",
				"october", "odor", "off", "offer", "office", "often", "oil", "okay", "old", "olive", "olympic",
				"omit", "once", "one", "onion", "online", "only", "open", "opera", "opinion", "oppose",
				"option", "orange", "orbit", "orchard", "order", "ordinary", "organ", "orient", "original",
				"orphan", "ostrich", "other", "outdoor", "outer", "output", "outside", "oval", "oven", "over",
				"own", "owner", "oxygen", "oyster", "ozone", "pact", "paddle", "page", "pair", "palace",
				"palm", "panda", "panel", "panic", "panther", "paper", "parade", "parent", "park", "parrot",
				"party", "pass", "patch", "path", "patient", "patrol", "pattern", "pause", "pave", "payment",
				"peace", "peanut", "pear", "peasant", "pelican", "pen", "penalty", "pencil", "people",
				"pepper", "perfect", "permit", "person", "pet", "phone", "photo", "phrase", "physical",
				"piano", "picnic", "picture", "piece", "pig", "pigeon", "pill", "pilot", "pink", "pioneer",
				"pipe", "pistol", "pitch", "pizza", "place", "planet", "plastic", "plate", "play", "please",
				"pledge", "pluck", "plug", "plunge", "poem", "poet", "point", "polar", "pole", "police",
				"pond", "pony", "pool", "popular", "portion", "position", "possible", "post", "potato",
				"pottery", "poverty", "powder", "power", "practice", "praise", "predict", "prefer", "prepare",
				"present", "pretty", "prevent", "price", "pride", "primary", "print", "priority", "prison",
				"private", "prize", "problem", "process", "produce", "profit", "program", "project", "promote",
				"proof", "property", "prosper", "protect", "proud", "provide", "public", "pudding", "pull",
				"pulp", "pulse", "pumpkin", "punch", "pupil", "puppy", "purchase", "purity", "purpose",
				"purse", "push", "put", "puzzle", "pyramid", "quality", "quantum", "quarter", "question",
				"quick", "quit", "quiz", "quote", "rabbit", "raccoon", "race", "rack", "radar", "radio",
				"rail", "rain", "raise", "rally", "ramp", "ranch", "random", "range", "rapid", "rare", "rate",
				"rather", "raven", "raw", "razor", "ready", "real", "reason", "rebel", "rebuild", "recall",
				"receive", "recipe", "record", "recycle", "reduce", "reflect", "reform", "refuse", "region",
				"regret", "regular", "reject", "relax", "release", "relief", "rely", "remain", "remember",
				"remind", "remove", "render", "renew", "rent", "reopen", "repair", "repeat", "replace",
				"report", "require", "rescue", "resemble", "resist", "resource", "response", "result",
				"retire", "retreat", "return", "reunion", "reveal", "review", "reward", "rhythm", "rib",
				"ribbon", "rice", "rich", "ride", "ridge", "rifle", "right", "rigid", "ring", "riot", "ripple",
				"risk", "ritual", "rival", "river", "road", "roast", "robot", "robust", "rocket", "romance",
				"roof", "rookie", "room", "rose", "rotate", "rough", "round", "route", "royal", "rubber",
				"rude", "rug", "rule", "run", "runway", "rural", "sad", "saddle", "sadness", "safe", "sail",
				"salad", "salmon", "salon", "salt", "salute", "same", "sample", "sand", "satisfy", "satoshi",
				"sauce", "sausage", "save", "say", "scale", "scan", "scare", "scatter", "scene", "scheme",
				"school", "science", "scissors", "scorpion", "scout", "scrap", "screen", "script", "scrub",
				"sea", "search", "season", "seat", "second", "secret", "section", "security", "seed", "seek",
				"segment", "select", "sell", "seminar", "senior", "sense", "sentence", "series", "service",
				"session", "settle", "setup", "seven", "shadow", "shaft", "shallow", "share", "shed", "shell",
				"sheriff", "shield", "shift", "shine", "ship", "shiver", "shock", "shoe", "shoot", "shop",
				"short", "shoulder", "shove", "shrimp", "shrug", "shuffle", "shy", "sibling", "sick", "side",
				"siege", "sight", "sign", "silent", "silk", "silly", "silver", "similar", "simple", "since",
				"sing", "siren", "sister", "situate", "six", "size", "skate", "sketch", "ski", "skill", "skin",
				"skirt", "skull", "slab", "slam", "sleep", "slender", "slice", "slide", "slight", "slim",
				"slogan", "slot", "slow", "slush", "small", "smart", "smile", "smoke", "smooth", "snack",
				"snake", "snap", "sniff", "snow", "soap", "soccer", "social", "sock", "soda", "soft", "solar",
				"soldier", "solid", "solution", "solve", "someone", "song", "soon", "sorry", "sort", "soul",
				"sound", "soup", "source", "south", "space", "spare", "spatial", "spawn", "speak", "special",
				"speed", "spell", "spend", "sphere", "spice", "spider", "spike", "spin", "spirit", "split",
				"spoil", "sponsor", "spoon", "sport", "spot", "spray", "spread", "spring", "spy", "square",
				"squeeze", "squirrel", "stable", "stadium", "staff", "stage", "stairs", "stamp", "stand",
				"start", "state", "stay", "steak", "steel", "stem", "step", "stereo", "stick", "still",
				"sting", "stock", "stomach", "stone", "stool", "story", "stove", "strategy", "street",
				"strike", "strong", "struggle", "student", "stuff", "stumble", "style", "subject", "submit",
				"subway", "success", "such", "sudden", "suffer", "sugar", "suggest", "suit", "summer", "sun",
				"sunny", "sunset", "super", "supply", "supreme", "sure", "surface", "surge", "surprise",
				"surround", "survey", "suspect", "sustain", "swallow", "swamp", "swap", "swarm", "swear",
				"sweet", "swift", "swim", "swing", "switch", "sword", "symbol", "symptom", "syrup", "system",
				"table", "tackle", "tag", "tail", "talent", "talk", "tank", "tape", "target", "task", "taste",
				"tattoo", "taxi", "teach", "team", "tell", "ten", "tenant", "tennis", "tent", "term", "test",
				"text", "thank", "that", "theme", "then", "theory", "there", "they", "thing", "this",
				"thought", "three", "thrive", "throw", "thumb", "thunder", "ticket", "tide", "tiger", "tilt",
				"timber", "time", "tiny", "tip", "tired", "tissue", "title", "toast", "tobacco", "today",
				"toddler", "toe", "together", "toilet", "token", "tomato", "tomorrow", "tone", "tongue",
				"tonight", "tool", "tooth", "top", "topic", "topple", "torch", "tornado", "tortoise", "toss",
				"total", "tourist", "toward", "tower", "town", "toy", "track", "trade", "traffic", "tragic",
				"train", "transfer", "trap", "trash", "travel", "tray", "treat", "tree", "trend", "trial",
				"tribe", "trick", "trigger", "trim", "trip", "trophy", "trouble", "truck", "true", "truly",
				"trumpet", "trust", "truth", "try", "tube", "tuition", "tumble", "tuna", "tunnel", "turkey",
				"turn", "turtle", "twelve", "twenty", "twice", "twin", "twist", "two", "type", "typical",
				"ugly", "umbrella", "unable", "unaware", "uncle", "uncover", "under", "undo", "unfair",
				"unfold", "unhappy", "uniform", "unique", "unit", "universe", "unknown", "unlock", "until",
				"unusual", "unveil", "update", "upgrade", "uphold", "upon", "upper", "upset", "urban", "urge",
				"usage", "use", "used", "useful", "useless", "usual", "utility", "vacant", "vacuum", "vague",
				"valid", "valley", "valve", "van", "vanish", "vapor", "various", "vast", "vault", "vehicle",
				"velvet", "vendor", "venture", "venue", "verb", "verify", "version", "very", "vessel",
				"veteran", "viable", "vibrant", "vicious", "victory", "video", "view", "village", "vintage",
				"violin", "virtual", "virus", "visa", "visit", "visual", "vital", "vivid", "vocal", "voice",
				"void", "volcano", "volume", "vote", "voyage", "wage", "wagon", "wait", "walk", "wall",
				"walnut", "want", "warfare", "warm", "warrior", "wash", "wasp", "waste", "water", "wave",
				"way", "wealth", "weapon", "wear", "weasel", "weather", "web", "wedding", "weekend", "weird",
				"welcome", "west", "wet", "whale", "what", "wheat", "wheel", "when", "where", "whip",
				"whisper", "wide", "width", "wife", "wild", "will", "win", "window", "wine", "wing", "wink",
				"winner", "winter", "wire", "wisdom", "wise", "wish", "witness", "wolf", "woman", "wonder",
				"wood", "wool", "word", "work", "world", "worry", "worth", "wrap", "wreck", "wrestle", "wrist",
				"write", "wrong", "yard", "year", "yellow", "you", "young", "youth", "zebra", "zero", "zone",
				"zoo"
		};

		assertEquals(2048, temp.length);
	}
	private void setupAddressBook() throws IOException {
		File addressBookFile = new File("src/test/resources/addressBook.bin");
		FileInputStream fis = new FileInputStream(addressBookFile);
		byte[] content = new byte[(int) addressBookFile.length()];
		fis.read(content);
		addressBook = NodeAddressBook.parseFrom(content);

		nodes.put("0.0.3", "35.237.182.66:50211");
		nodes.put("0.0.4", "35.245.226.22:50211");
		nodes.put("0.0.5", "34.68.9.203:50211");
		nodes.put("0.0.6", "34.83.131.197:50211");
		nodes.put("0.0.7", "34.94.236.63:50211");
		nodes.put("0.0.8", "35.203.26.115:50211");
		nodes.put("0.0.9", "34.77.3.213:50211");
		nodes.put("0.0.10", "35.197.237.44:50211");
		nodes.put("0.0.11", "35.246.250.176:50211");
		nodes.put("0.0.12", "34.90.117.105:50211");
		nodes.put("0.0.13", "35.200.57.21:50211");
		nodes.put("0.0.14", "34.92.120.143:50211");
		nodes.put("0.0.15", "34.87.47.168:50211");
	}
}
