package com.ams.linkme.domain

import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

data class InterestTopic(
    val id: String,
    val label: String,
    val groupId: String,
    val aliases: List<String> = emptyList(),
    val isBroad: Boolean = false,
)

data class InterestGroup(val id: String, val label: String)

data class InterestSuggestion(val label: String, val reason: String)

/** Evidence is recomputed from labels; persisted query keys never participate in a match. */
data class InterestMatch(val exact: List<String>, val relatedGroups: List<String>, val score: Int)

data class ExplorationChoice(
    val id: String,
    val title: String,
    val detail: String,
    val topics: List<String>,
)

data class ExplorationQuestion(val title: String, val choices: List<ExplorationChoice>)

/**
 * Bundled, editable starting points and deterministic suggestions, with no Android or network use.
 */
object InterestCatalog {
    const val MAX_INTERESTS = 16
    const val MAX_LABEL_LENGTH = 40

    val groups =
        listOf(
            InterestGroup("sports", "Sports & movement"),
            InterestGroup("games", "Games"),
            InterestGroup("food", "Food & drink"),
            InterestGroup("music", "Music"),
            InterestGroup("screen", "Film & animation"),
            InterestGroup("creative", "Creative arts"),
            InterestGroup("outdoors", "Outdoors & travel"),
            InterestGroup("tech", "Technology & science"),
            InterestGroup("learning", "Books & learning"),
            InterestGroup("social", "Social & culture"),
            InterestGroup("wellbeing", "Wellbeing"),
            InterestGroup("community", "Community & causes"),
        )

    val topics =
        listOf(
            topic("sport", "Sport", "sports", "Sports", "体育", isBroad = true),
            topic("basketball", "Basketball", "sports", "篮球"),
            topic("soccer", "Soccer", "sports", "Football", "足球"),
            topic("volleyball", "Volleyball", "sports", "排球"),
            topic("badminton", "Badminton", "sports", "羽毛球"),
            topic("tennis", "Tennis", "sports", "网球"),
            topic("running", "Running", "sports", "Jogging", "跑步"),
            topic("swimming", "Swimming", "sports", "游泳"),
            topic("cycling", "Cycling", "sports", "Biking", "骑行"),
            topic("bouldering", "Bouldering", "sports", "Indoor climbing", "抱石"),
            topic("table-tennis", "Table tennis", "sports", "Ping pong", "乒乓球"),
            topic("skateboarding", "Skateboarding", "sports", "滑板"),
            topic("ultimate-frisbee", "Ultimate frisbee", "sports", "飞盘"),
            topic("games", "Games", "games", "Gaming", "游戏", isBroad = true),
            topic("indie-games", "Indie games", "games", "Indie gaming", "独立游戏"),
            topic("board-games", "Board games", "games", "Tabletop games", "桌游"),
            topic("co-op-games", "Co-op games", "games", "Cooperative games", "Coop games", "合作游戏"),
            topic("role-playing-games", "Role-playing games", "games", "RPGs", "Roleplaying games"),
            topic("strategy-games", "Strategy games", "games", "策略游戏"),
            topic("puzzle-games", "Puzzle games", "games", "解谜游戏"),
            topic("nintendo-games", "Nintendo games", "games", "Nintendo", "任天堂"),
            topic("pc-gaming", "PC gaming", "games", "PC games"),
            topic("console-gaming", "Console gaming", "games", "Console games", "主机游戏"),
            topic("chess", "Chess", "games", "国际象棋"),
            topic("dungeons-dragons", "Dungeons & Dragons", "games", "Dungeons and Dragons", "D&D"),
            topic("esports", "Esports", "games", "E-sports", "电子竞技"),
            topic("food", "Food", "food", "美食", isBroad = true),
            topic("cooking", "Cooking", "food", "做饭", "烹饪"),
            topic("baking", "Baking", "food", "烘焙"),
            topic("coffee", "Coffee", "food", "咖啡"),
            topic("tea", "Tea", "food", "品茶"),
            topic("street-food", "Street food", "food", "街头美食"),
            topic("vegetarian-cooking", "Vegetarian cooking", "food", "Vegetarian food", "素食烹饪"),
            topic("desserts", "Desserts", "food", "Dessert", "甜点"),
            topic("food-markets", "Food markets", "food", "Farmers markets", "菜市场"),
            topic("fermentation", "Fermentation", "food", "发酵"),
            topic("home-cooking", "Home cooking", "food", "家常菜"),
            topic("noodles", "Noodles", "food", "面食"),
            topic("barbecue", "Barbecue", "food", "BBQ", "烧烤"),
            topic("music", "Music", "music", "音乐", isBroad = true),
            topic("live-music", "Live music", "music", "Concerts", "Live shows", "现场音乐"),
            topic("indie-rock", "Indie rock", "music", "独立摇滚"),
            topic("vinyl-records", "Vinyl records", "music", "Vinyl", "Record collecting", "黑胶唱片"),
            topic("jazz", "Jazz", "music", "爵士乐"),
            topic("hip-hop", "Hip-hop", "music", "Hip hop", "嘻哈"),
            topic("electronic-music", "Electronic music", "music", "EDM", "电子音乐"),
            topic("classical-music", "Classical music", "music", "古典音乐"),
            topic("k-pop", "K-pop", "music", "Kpop", "韩流音乐"),
            topic("playing-guitar", "Playing guitar", "music", "Guitar", "弹吉他"),
            topic("piano", "Piano", "music", "钢琴"),
            topic("songwriting", "Songwriting", "music", "歌曲创作"),
            topic("choir", "Choir", "music", "Choral singing", "合唱"),
            topic("movie", "Movie", "screen", "Movies", "Film", "Films", "电影", isBroad = true),
            topic("anime", "Anime", "screen", "动漫", "日本动画"),
            topic("manga", "Manga", "screen", "漫画"),
            topic("animation", "Animation", "screen", "动画"),
            topic("documentaries", "Documentaries", "screen", "Documentary", "纪录片"),
            topic("sci-fi-movies", "Sci-fi movies", "screen", "Science fiction movies", "科幻电影"),
            topic("horror-movies", "Horror movies", "screen", "Horror films", "恐怖电影"),
            topic("comedy-shows", "Comedy shows", "screen", "Stand-up comedy", "脱口秀"),
            topic("film-festivals", "Film festivals", "screen", "电影节"),
            topic("tv-dramas", "TV dramas", "screen", "Television dramas", "电视剧"),
            topic("studio-ghibli", "Studio Ghibli", "screen", "Ghibli", "吉卜力"),
            topic("short-films", "Short films", "screen", "电影短片"),
            topic("photography", "Photography", "creative", "Take photos", "Taking photos", "摄影"),
            topic("drawing", "Drawing", "creative", "Sketching", "画画", "素描"),
            topic("painting", "Painting", "creative", "绘画"),
            topic("pottery", "Pottery", "creative", "Ceramics", "陶艺"),
            topic("creative-writing", "Creative writing", "creative", "Writing stories", "创意写作"),
            topic("graphic-design", "Graphic design", "creative", "平面设计"),
            topic("diy-projects", "DIY projects", "creative", "DIY", "手工制作"),
            topic("knitting", "Knitting", "creative", "编织"),
            topic("sewing", "Sewing", "creative", "缝纫"),
            topic("digital-art", "Digital art", "creative", "数字艺术"),
            topic("dance", "Dance", "creative", "Dancing", "舞蹈"),
            topic("theater", "Theater", "creative", "Theatre", "戏剧"),
            topic("hiking", "Hiking", "outdoors", "徒步", "远足"),
            topic("camping", "Camping", "outdoors", "露营"),
            topic("city-walks", "City walks", "outdoors", "City walking", "城市漫步"),
            topic("birdwatching", "Birdwatching", "outdoors", "Bird watching", "观鸟"),
            topic("gardening", "Gardening", "outdoors", "园艺"),
            topic("travel", "Travel", "outdoors", "Travelling", "Traveling", "旅行", "旅游"),
            topic("backpacking", "Backpacking", "outdoors", "背包旅行"),
            topic("urban-exploration", "Urban exploration", "outdoors", "城市探索"),
            topic("road-trips", "Road trips", "outdoors", "Road trip", "自驾游"),
            topic("nature-walks", "Nature walks", "outdoors", "Nature walking", "自然漫步"),
            topic("beach-trips", "Beach trips", "outdoors", "海边旅行"),
            topic("national-parks", "National parks", "outdoors", "国家公园"),
            topic("astrophotography", "Astrophotography", "tech", "Night sky photography", "天文摄影"),
            topic("programming", "Programming", "tech", "Coding", "编程"),
            topic("robotics", "Robotics", "tech", "机器人"),
            topic("game-development", "Game development", "tech", "Making games", "游戏开发"),
            topic("3d-printing", "3D printing", "tech", "3-D printing", "3D打印"),
            topic("web-development", "Web development", "tech", "Building websites", "网页开发"),
            topic("open-source-software", "Open-source software", "tech", "Open source", "开源软件"),
            topic("data-science", "Data science", "tech", "数据科学"),
            topic("electronics", "Electronics", "tech", "电子制作"),
            topic("astronomy", "Astronomy", "tech", "天文学"),
            topic("app-development", "App development", "tech", "Building apps", "应用开发"),
            topic("cybersecurity", "Cybersecurity", "tech", "Information security", "网络安全"),
            topic("reading", "Reading", "learning", "Books", "阅读", "读书"),
            topic("fantasy-books", "Fantasy books", "learning", "Fantasy novels", "奇幻小说"),
            topic(
                "science-fiction-books",
                "Science fiction books",
                "learning",
                "Sci-fi books",
                "科幻小说",
            ),
            topic("history", "History", "learning", "历史"),
            topic(
                "language-learning",
                "Language learning",
                "learning",
                "Learning languages",
                "语言学习",
            ),
            topic("philosophy", "Philosophy", "learning", "哲学"),
            topic("psychology", "Psychology", "learning", "心理学"),
            topic("science-podcasts", "Science podcasts", "learning", "科学播客"),
            topic("nonfiction", "Nonfiction", "learning", "Non-fiction", "非虚构阅读"),
            topic("book-clubs", "Book clubs", "learning", "Book club", "读书会"),
            topic("economics", "Economics", "learning", "经济学"),
            topic("poetry", "Poetry", "learning", "诗歌"),
            topic("dating", "Dating", "social", "约会", isBroad = true),
            topic("language-exchange", "Language exchange", "social", "语言交换"),
            topic("meetups", "Meetups", "social", "Meetup groups", "线下聚会"),
            topic("karaoke", "Karaoke", "social", "KTV", "卡拉OK"),
            topic("trivia-nights", "Trivia nights", "social", "Pub quizzes", "知识问答"),
            topic("picnics", "Picnics", "social", "Picnic", "野餐"),
            topic("dinner-parties", "Dinner parties", "social", "聚餐"),
            topic("cultural-exchange", "Cultural exchange", "social", "文化交流"),
            topic("study-groups", "Study groups", "social", "学习小组"),
            topic("coffee-chats", "Coffee chats", "social", "咖啡聊天"),
            topic("pen-pals", "Pen pals", "social", "Penpals", "笔友"),
            topic("yoga", "Yoga", "wellbeing", "瑜伽"),
            topic("meditation", "Meditation", "wellbeing", "冥想"),
            topic("mindfulness", "Mindfulness", "wellbeing", "正念"),
            topic("fitness", "Fitness", "wellbeing", "Working out", "健身"),
            topic("pilates", "Pilates", "wellbeing", "普拉提"),
            topic("journaling", "Journaling", "wellbeing", "Keeping a journal", "写日记"),
            topic("relaxed-walks", "Relaxed walks", "wellbeing", "散步"),
            topic("strength-training", "Strength training", "wellbeing", "Weightlifting", "力量训练"),
            topic("stretching", "Stretching", "wellbeing", "拉伸"),
            topic("sleep-habits", "Sleep habits", "wellbeing", "Sleep routines", "睡眠习惯"),
            topic("mental-wellbeing", "Mental wellbeing", "wellbeing", "心理健康"),
            topic("self-care", "Self-care", "wellbeing", "Self care", "自我关怀"),
            topic("other", "Other", "community", "其他", isBroad = true),
            topic("volunteering", "Volunteering", "community", "Volunteer work", "志愿服务"),
            topic("sustainability", "Sustainability", "community", "可持续生活"),
            topic("animal-welfare", "Animal welfare", "community", "动物保护"),
            topic("local-events", "Local events", "community", "社区活动"),
            topic("community-gardens", "Community gardens", "community", "社区花园"),
            topic("environmental-action", "Environmental action", "community", "环保行动"),
            topic("mentoring", "Mentoring", "community", "指导新人"),
            topic("neighborhood-projects", "Neighborhood projects", "community", "邻里项目"),
            topic("museum-visits", "Museum visits", "community", "Museums", "博物馆"),
            topic("pet-care", "Pet care", "community", "Caring for pets", "宠物照顾"),
            topic("accessibility", "Accessibility", "community", "无障碍"),
            topic("student-clubs", "Student clubs", "community", "校园社团"),
        )

    val questions =
        listOf(
            ExplorationQuestion(
                "An open afternoon appears. What sounds good?",
                listOf(
                    choice(
                        "afternoon-games",
                        "Try a game",
                        "A relaxed co-op session or a game night",
                        "Indie games",
                        "Board games",
                        "Co-op games",
                    ),
                    choice(
                        "afternoon-outdoors",
                        "Head outside",
                        "A nearby trail, a city walk or a bike ride",
                        "Hiking",
                        "City walks",
                        "Cycling",
                    ),
                    choice(
                        "afternoon-making",
                        "Make something",
                        "Work with your hands or try a new recipe",
                        "Pottery",
                        "Photography",
                        "Cooking",
                    ),
                    choice(
                        "afternoon-live",
                        "Catch something live",
                        "A small gig, a play or a new band",
                        "Live music",
                        "Indie rock",
                        "Theater",
                    ),
                ),
            ),
            ExplorationQuestion(
                "What could keep a conversation going?",
                listOf(
                    choice(
                        "conversation-stories",
                        "Stories and imagined worlds",
                        "Trade a favorite film, series or book",
                        "Anime",
                        "Movie",
                        "Fantasy books",
                    ),
                    choice(
                        "conversation-how",
                        "How things work",
                        "A project, a discovery or a question",
                        "Astrophotography",
                        "Programming",
                        "Science podcasts",
                    ),
                    choice(
                        "conversation-flavors",
                        "A new flavor",
                        "A recipe, a cafe or a favorite food stall",
                        "Coffee",
                        "Cooking",
                        "Street food",
                    ),
                    choice(
                        "conversation-places",
                        "Places and perspectives",
                        "Trips, languages and things around the corner",
                        "Travel",
                        "Language exchange",
                        "Urban exploration",
                    ),
                ),
            ),
            ExplorationQuestion(
                "How do you like to spend time with people?",
                listOf(
                    choice(
                        "together-small",
                        "Keep it small",
                        "A table, a book and time to chat",
                        "Board games",
                        "Coffee",
                        "Book clubs",
                    ),
                    choice(
                        "together-active",
                        "Move together",
                        "A friendly match or a route to explore",
                        "Badminton",
                        "Bouldering",
                        "Hiking",
                    ),
                    choice(
                        "together-project",
                        "Work on something",
                        "Make, learn or help alongside someone",
                        "Drawing",
                        "DIY projects",
                        "Volunteering",
                    ),
                    choice(
                        "together-culture",
                        "Find a little culture",
                        "A museum visit, a show or a photo walk",
                        "Museum visits",
                        "Live music",
                        "Photography",
                    ),
                ),
            ),
        )

    private val topicByName = buildMap {
        // A topic's canonical name takes precedence if an alias ever overlaps another label.
        topics.forEach { put(fold(it.label), it) }
        topics.forEach { topic -> topic.aliases.forEach { putIfAbsent(fold(it), topic) } }
    }
    private val groupById = groups.associateBy { it.id }
    private val legacyRoots =
        mapOf(
            "sports" to listOf("Sport", "Sports"),
            "games" to listOf("Games"),
            "food" to listOf("Food"),
            "music" to listOf("Music"),
            "screen" to listOf("Anime", "Movie", "Movies"),
            "social" to listOf("Dating"),
            "community" to listOf("Other"),
        )
    private val clauseBreak =
        Regex(
            "[.!?;。！？；]|\\b(?:but|however|yet)\\b|\\bi\\s+(?=(?:love|like|enjoy|prefer|play|watch|read|collect|am\\s+into)\\b)|[,，]\\s*(?=(?:我|也|还)?(?:喜欢|热爱|爱好))|但是|不过|但"
        )
    private val negativeCue =
        Regex(
            "\\b(?:do not|does not|did not|don't|doesn't|didn't|not|never|no longer|hate|dislike|avoid|without|no|used\\s+to)\\b|不喜欢|不爱|不想|不感兴趣|讨厌|不玩|不看|没兴趣|不热衷"
        )
    private val negativeAfter =
        Regex(
            "^\\s*(?:(?:is|are|was|were)\\s+not\\b|isn't\\b|aren't\\b|wasn't\\b|weren't\\b|doesn't\\b|does not\\b|not for me\\b|no thanks\\b|(?:我)?(?:不喜欢|没兴趣|不感兴趣))"
        )

    /** Returns canonical known labels and the first display spelling of each valid custom label. */
    fun normalize(values: Collection<String>): List<String> {
        val result = linkedMapOf<String, String>()
        for (raw in values) {
            if (validateLabel(raw) != null) continue
            val clean = clean(raw)
            val canonical =
                WikiTopicReferences.canonical(clean) ?: topicByName[fold(clean)]?.label ?: clean
            result.putIfAbsent(fold(canonical), canonical)
            if (result.size == MAX_INTERESTS) break
        }
        return result.values.toList()
    }

    fun label(value: String): String {
        WikiTopicReferences.label(clean(value))?.let {
            return it
        }
        val canonical = topicByName[fold(value)]?.label ?: clean(value)
        return when (canonical) {
            "Sport" -> "Sports"
            "Movie" -> "Movies"
            else -> canonical
        }
    }

    /** Null means valid. Length is measured after normalization, using UTF-16 code units. */
    fun validateLabel(value: String): String? {
        if (value.any { it.code <= 0x1f || it.code in 0x7f..0x9f }) {
            return "Keep each interest on one line, without control characters."
        }
        if (hasUnpairedSurrogate(value)) return "Use a valid text label."
        val normalized = clean(value)
        if (normalized.isEmpty() || !hasVisibleContent(normalized)) return "Add an interest name."
        if (normalized.length > MAX_LABEL_LENGTH) return "Keep each interest to 40 characters."
        return null
    }

    fun search(query: String, groupId: String? = null, limit: Int = 12): List<InterestTopic> {
        if (limit <= 0 || (groupId != null && groupId !in groupById)) return emptyList()
        val needle = fold(query)
        return topics
            .asSequence()
            .filter { groupId == null || it.groupId == groupId }
            .mapNotNull { topic -> searchRank(topic, needle)?.let { rank -> topic to rank } }
            .sortedWith(
                compareBy<Pair<InterestTopic, Int>> { it.second }
                    .thenBy { it.first.isBroad }
                    .thenBy { fold(label(it.first.label)) }
            )
            .take(limit)
            .map { it.first }
            .toList()
    }

    /** These are candidates for an explicit tap; this method never changes a selected mix. */
    fun suggestions(
        selected: List<String>,
        bio: String,
        explorationChoices: Set<String> = emptySet(),
        limit: Int = 8,
    ): List<InterestSuggestion> {
        if (limit <= 0) return emptyList()
        val selectedLabels = normalize(selected)
        val seen = selectedLabels.map { fold(it) }.toMutableSet()
        val mentions = textMentions(bio)
        val positiveMentions = mentions.filterNot { it.negated }
        val excluded =
            mentions.filter { it.negated }.map { it.topic.id }.toSet() -
                positiveMentions.map { it.topic.id }.toSet()
        val result = mutableListOf<InterestSuggestion>()
        fun add(value: String, reason: String) {
            val topic = topicByName[fold(value)] ?: return
            if (topic.id !in excluded && result.size < limit && seen.add(fold(topic.label))) {
                result += InterestSuggestion(topic.label, reason)
            }
        }
        positiveMentions.forEach { add(it.topic.label, "Mentioned in your introduction") }
        questions
            .flatMap { it.choices }
            .filter { it.id in explorationChoices }
            .forEach { choice ->
                choice.topics.forEach { add(it, "From your scene choice: ${choice.title}") }
            }
        val selectedGroups = selectedLabels.mapNotNull { topicByName[fold(it)]?.groupId }.distinct()
        val relatedByGroup =
            selectedGroups.map { groupId ->
                topics
                    .filter {
                        it.groupId == groupId &&
                            !it.isBroad &&
                            it.id !in excluded &&
                            fold(it.label) !in seen
                    }
                    .iterator()
            }
        // Give each selected category a turn before offering another topic from the same one.
        while (result.size < limit && relatedByGroup.any { it.hasNext() }) {
            for (candidates in relatedByGroup) {
                if (result.size == limit) break
                if (candidates.hasNext()) {
                    val topic = candidates.next()
                    add(topic.label, "Explore more in ${groupById.getValue(topic.groupId).label}")
                }
            }
        }
        return result
    }

    /**
     * Literal mentions only, with conservative negation handling; no facts inferred about a person.
     */
    fun fromText(text: String): List<String> =
        normalize(textMentions(text).filterNot { it.negated }.map { it.topic.label })

    private data class Mention(
        val topic: InterestTopic,
        val start: Int,
        val end: Int,
        val negated: Boolean,
    )

    private fun textMentions(text: String): List<Mention> {
        val normalized = fold(text).replace('’', '\'')
        if (normalized.isEmpty()) return emptyList()
        val mentions = mutableListOf<Mention>()
        for (topic in topics) {
            for (name in (listOf(topic.label) + topic.aliases).map(::fold).distinct()) {
                val phrase = Regex.escape(name)
                val pattern =
                    if (
                        name.any {
                            Character.UnicodeScript.of(it.code) == Character.UnicodeScript.HAN
                        }
                    )
                        Regex(phrase)
                    else Regex("(?<![\\p{L}\\p{N}_])$phrase(?![\\p{L}\\p{N}_])")
                pattern.findAll(normalized).forEach { found ->
                    mentions +=
                        Mention(
                            topic,
                            found.range.first,
                            found.range.last,
                            isNegated(normalized, found.range),
                        )
                }
            }
        }
        val accepted = mutableListOf<Mention>()
        mentions
            .sortedWith(compareBy<Mention> { it.start }.thenByDescending { it.end - it.start })
            .forEach { mention ->
                if (accepted.none { mention.start <= it.end && mention.end >= it.start })
                    accepted += mention
            }
        return accepted
    }

    fun indexKeys(values: Collection<String>): List<String> {
        val labels = normalize(values)
        val keys = linkedSetOf<String>()
        val groupIds = linkedSetOf<String>()
        labels.forEach { value ->
            val topic = topicByName[fold(value)]
            if (topic == null) keys += "c:${sha256(fold(value))}"
            else {
                keys += "t:${topic.id}"
                groupIds += topic.groupId
            }
        }
        groupIds.forEach { keys += "g:$it" }
        return keys.take(30)
    }

    fun legacyQueryValues(values: Collection<String>): List<String> {
        val labels = normalize(values)
        val result = labels.toMutableSet()
        labels
            .mapNotNull { topicByName[fold(it)]?.groupId }
            .distinct()
            .forEach { groupId -> result.addAll(legacyRoots[groupId].orEmpty()) }
        return result.take(30)
    }

    fun match(left: Collection<String>, right: Collection<String>): InterestMatch {
        val leftLabels = normalize(left)
        val rightLabels = normalize(right)
        val rightNames = rightLabels.map { fold(it) }.toSet()
        val exact = leftLabels.filter { fold(it) in rightNames }
        val leftGroups = leftLabels.mapNotNull { topicByName[fold(it)]?.groupId }.toSet()
        val rightGroups = rightLabels.mapNotNull { topicByName[fold(it)]?.groupId }.toSet()
        val related = groups.filter { it.id in leftGroups && it.id in rightGroups }.map { it.label }
        val specificCount = exact.count { topicByName[fold(it)]?.isBroad != true }
        val broadCount = exact.size - specificCount
        // One specific/custom exact match outweighs every possible broad match/category combined.
        return InterestMatch(
            exact,
            related,
            specificCount * 10_000 + broadCount * 100 + related.size,
        )
    }

    private fun searchRank(topic: InterestTopic, query: String): Int? {
        if (query.isEmpty()) return 0
        val name = fold(label(topic.label))
        val aliases = (listOf(topic.label) + topic.aliases).map(::fold)
        return when {
            name == query || query in aliases -> 0
            name.startsWith(query) -> 1
            aliases.any { it.startsWith(query) } -> 2
            query.split(' ').all { it in name } -> 3
            aliases.any { query in it } -> 4
            else -> null
        }
    }

    private fun isNegated(text: String, range: IntRange): Boolean {
        val clauseStart =
            clauseBreak.findAll(text.substring(0, range.first)).lastOrNull()?.range?.last?.plus(1)
                ?: 0
        val prefix = text.substring(clauseStart, range.first)
        if (negativeCue.containsMatchIn(prefix)) return true
        return negativeAfter.containsMatchIn(text.substring(range.last + 1))
    }

    private fun clean(value: String): String {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFKC)
        val result = StringBuilder()
        var pendingSpace = false
        var offset = 0
        while (offset < normalized.length) {
            val cp = Character.codePointAt(normalized, offset)
            if (Character.isWhitespace(cp) || Character.isSpaceChar(cp)) {
                if (result.isNotEmpty()) pendingSpace = true
            } else {
                if (pendingSpace) result.append(' ')
                result.appendCodePoint(cp)
                pendingSpace = false
            }
            offset += Character.charCount(cp)
        }
        return result.toString()
    }

    private fun fold(value: String): String = clean(value).lowercase(Locale.ROOT)

    private fun hasVisibleContent(value: String): Boolean {
        var offset = 0
        while (offset < value.length) {
            val cp = Character.codePointAt(value, offset)
            val type = Character.getType(cp)
            if (
                !Character.isWhitespace(cp) &&
                    !Character.isSpaceChar(cp) &&
                    type != Character.FORMAT.toInt() &&
                    type != Character.NON_SPACING_MARK.toInt() &&
                    type != Character.ENCLOSING_MARK.toInt()
            )
                return true
            offset += Character.charCount(cp)
        }
        return false
    }

    private fun hasUnpairedSurrogate(value: String): Boolean {
        var offset = 0
        while (offset < value.length) {
            val char = value[offset]
            if (Character.isHighSurrogate(char)) {
                if (offset + 1 >= value.length || !Character.isLowSurrogate(value[offset + 1]))
                    return true
                offset += 2
            } else {
                if (Character.isLowSurrogate(char)) return true
                offset++
            }
        }
        return false
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString(
            ""
        ) {
            "%02x".format(it)
        }

    private fun topic(
        id: String,
        label: String,
        groupId: String,
        vararg aliases: String,
        isBroad: Boolean = false,
    ) = InterestTopic(id, label, groupId, aliases.toList(), isBroad)

    private fun choice(id: String, title: String, detail: String, vararg topics: String) =
        ExplorationChoice(id, title, detail, topics.toList())
}
