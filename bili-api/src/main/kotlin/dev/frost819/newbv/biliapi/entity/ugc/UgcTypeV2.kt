package dev.frost819.newbv.biliapi.entity.ugc

enum class UgcTypeV2(val tid: Int, val codename: String, val channelId: Int? = null) {
    // 动画
    Douga(1005, "douga", 7),
    DougaFanAnime(2037, "fan_anime"),
    DougaGarageKit(2038, "garage_kit"),
    DougaCosplay(2039, "cosplay"),
    DougaOffline(2040, "offline"),
    DougaEditing(2041, "editing"),
    DougaCommentary(2042, "commentary"),
    DougaQuickView(2043, "quick_view"),
    DougaVoice(2044, "voice"),
    DougaInformation(2045, "information"),
    DougaInterpret(2046, "interpret"),
    DougaVup(2047, "vup"),
    DougaTokusatsu(2048, "tokusatsu"),
    DougaPuppetry(2049, "puppetry"),
    DougaComic(2050, "comic"),
    DougaMotion(2051, "motion"),
    DougaReaction(2052, "reaction"),
    DougaTutorial(2053, "tutorial"),
    DougaOther(2054, "other"),

    // 游戏
    Game(1008, "game", 8),
    GameRpg(2064, "rpg"),
    GameMmorpg(2065, "mmorpg"),
    GameStandAlone(2066, "stand_alone"),
    GameSlg(2067, "slg"),
    GameTbs(2068, "tbs"),
    GameRts(2069, "rts"),
    GameMoba(2070, "moba"),
    GameStg(2071, "stg"),
    GameSpg(2072, "spg"),
    GameAct(2073, "act"),
    GameMsc(2074, "msc"),
    GameSim(2075, "sim"),
    GameOtome(2076, "otome"),
    GamePuz(2077, "puz"),
    GameSandbox(2078, "sandbox"),
    GameOther(2079, "other"),

    // 鬼畜
    Kichiku(1007, "kichiku", 9),
    KichikuGuide(2059, "guide"),
    KichikuTheatre(2060, "theatre"),
    KichikuManualVocaloid(2061, "manual_vocaloid"),
    KichikuMad(2062, "mad"),
    KichikuOther(2063, "other"),

    // 音乐
    Music(1003, "music", 10),
    MusicOriginal(2016, "original"),
    MusicMv(2017, "mv"),
    MusicLive(2018, "live"),
    MusicFanVideos(2019, "fan_videos"),
    MusicCover(2020, "cover"),
    MusicPerform(2021, "perform"),
    MusicVocaloid(2022, "vocaloid"),
    MusicAiMusic(2023, "ai_music"),
    MusicRadio(2024, "radio"),
    MusicTutorial(2025, "tutorial"),
    MusicCommentary(2026, "commentary"),
    MusicOther(2027, "other"),

    // 舞蹈
    Dance(1004, "dance", 11),
    DanceOtaku(2028, "otaku"),
    DanceHiphop(2029, "hiphop"),
    DanceGestures(2030, "gestures"),
    DanceStar(2031, "star"),
    DanceChina(2032, "china"),
    DanceTutorial(2033, "tutorial"),
    DanceBallet(2034, "ballet"),
    DanceWota(2035, "wota"),
    DanceOther(2036, "other"),

    // 影视
    Cinephile(1001, "cinephile", 12),
    CinephileCommentary(2001, "commentary"),
    CinephileMontage(2002, "montage"),
    CinephileInformation(2003, "information"),
    CinephilePorterage(2004, "porterage"),
    CinephileShortFilm(2005, "shortfilm"),
    CinephileAi(2006, "ai"),
    CinephileReaction(2007, "reaction"),
    CinephileOther(2008, "other"),

    // 娱乐
    Ent(1002, "ent", 13),
    EntCommentary(2009, "commentary"),
    EntMontage(2010, "montage"),
    EntFansVideo(2011, "fans_video"),
    EntInformation(2012, "information"),
    EntReaction(2013, "reaction"),
    EntVariety(2014, "variety"),
    EntOther(2015, "other"),

    // 知识
    Knowledge(1010, "knowledge", 14),
    KnowledgeExam(2084, "exam"),
    KnowledgeLangSkill(2085, "lang_skill"),
    KnowledgeCampus(2086, "campus"),
    KnowledgeBusiness(2087, "business"),
    KnowledgeSocialObservation(2088, "social_observation"),
    KnowledgePolitics(2089, "politics"),
    KnowledgeHumanityHistory(2090, "humanity_history"),
    KnowledgeDesign(2091, "design"),
    KnowledgePsychology(2092, "psychology"),
    KnowledgeCareer(2093, "career"),
    KnowledgeScience(2094, "science"),
    KnowledgeOther(2095, "other"),

    // 科技数码
    Tech(1012, "tech", 15),
    TechComputer(2099, "computer"),
    TechPhone(2100, "phone"),
    TechPad(2101, "pad"),
    TechPhotography(2102, "photography"),
    TechMachine(2103, "machine"),
    TechCreate(2104, "create"),
    TechOther(2105, "other"),

    // 资讯
    Information(1009, "information", 16),
    InformationPolitics(2080, "politics"),
    InformationOverseas(2081, "overseas"),
    InformationSocial(2082, "social"),
    InformationOther(2083, "other"),

    // 美食
    Food(1020, "food", 17),
    FoodMake(2149, "make"),
    FoodDetective(2150, "detective"),
    FoodCommentary(2151, "commentary"),
    FoodRecord(2152, "record"),
    FoodOther(2153, "other"),

    // 小剧场
    Shortplay(1021, "shortplay", 18),
    ShortplayPlot(2154, "plot"),
    ShortplayLang(2155, "lang"),
    ShortplayUpVariety(2156, "up_variety"),
    ShortplayInterview(2157, "interview"),

    // 汽车
    Car(1013, "car", 19),
    CarCommentary(2106, "commentary"),
    CarCulture(2107, "culture"),
    CarLife(2108, "life"),
    CarTech(2109, "tech"),
    CarOther(2110, "other"),

    // 时尚美妆
    Fashion(1014, "fashion", 20),
    FashionMakeup(2111, "makeup"),
    FashionSkincare(2112, "skincare"),
    FashionCos(2113, "cos"),
    FashionOutfits(2114, "outfits"),
    FashionAccessories(2115, "accessories"),
    FashionJewelry(2116, "jewelry"),
    FashionTrick(2117, "trick"),
    FashionCommentary(2118, "commentary"),
    FashionOther(2119, "other"),

    // 体育运动
    Sports(1018, "sports", 21),
    SportsTrend(2133, "trend"),
    SportsFootball(2134, "football"),
    SportsBasketball(2135, "basketball"),
    SportsRunning(2136, "running"),
    SportsKungfu(2137, "kungfu"),
    SportsFighting(2138, "fighting"),
    SportsBadminton(2139, "badminton"),
    SportsInformation(2140, "information"),
    SportsMatch(2141, "match"),
    SportsOther(2142, "other"),

    // 动物
    Animal(1024, "animal", 22),
    AnimalCat(2167, "cat"),
    AnimalDog(2168, "dog"),
    AnimalReptiles(2169, "reptiles"),
    AnimalScience(2170, "science"),
    AnimalOther(2171, "other"),

    // vlog
    Vlog(1029, "vlog", 23),
    VlogLife(2194, "life"),
    VlogStudent(2195, "student"),
    VlogCareer(2196, "career"),
    VlogOther(2197, "other"),

    // 绘画
    Painting(1006, "painting", 24),
    PaintingAcg(2055, "acg"),
    PaintingNoneAcg(2056, "none_acg"),
    PaintingTutorial(2057, "tutorial"),
    PaintingOther(2058, "other"),

    // 人工智能
    Ai(1011, "ai", 25),
    AiTutorial(2096, "tutorial"),
    AiInformation(2097, "information"),
    AiOther(2098, "other"),

    // 家装房产
    Home(1015, "home", 26),
    HomeTrade(2120, "trade"),
    HomeRenovation(2121, "renovation"),
    HomeFurniture(2122, "furniture"),
    HomeAppliances(2123, "appliances"),

    // 户外潮流
    Outdoors(1016, "outdoors", 27),
    OutdoorsCamping(2124, "camping"),
    OutdoorsHiking(2125, "hiking"),
    OutdoorsExplore(2126, "explore"),
    OutdoorsOther(2127, "other"),

    // 健身
    Gym(1017, "gym", 28),
    GymScience(2128, "science"),
    GymTutorial(2129, "tutorial"),
    GymRecord(2130, "record"),
    GymFigure(2131, "figure"),
    GymOther(2132, "other"),

    // 手工
    Handmake(1019, "handmake", 29),
    HandmakeHandbook(2143, "handbook"),
    HandmakeLight(2144, "light"),
    HandmakeTraditional(2145, "traditional"),
    HandmakeRelief(2146, "relief"),
    HandmakeDiy(2147, "diy"),
    HandmakeOther(2148, "other"),

    // 旅游出行
    Travel(1022, "travel", 30),
    TravelRecord(2158, "record"),
    TravelStrategy(2159, "strategy"),
    TravelCity(2160, "city"),
    TravelTransport(2161, "transport"),

    // 三农
    Rural(1023, "rural", 31),
    RuralPlanting(2162, "planting"),
    RuralFishing(2163, "fishing"),
    RuralHarvest(2164, "harvest"),
    RuralTech(2165, "tech"),
    RuralLife(2166, "life"),

    // 亲子
    Parenting(1025, "parenting", 32),
    ParentingPregnantCare(2172, "pregnant_care"),
    ParentingInfantCare(2173, "infant_care"),
    ParentingTalent(2174, "talent"),
    ParentingCute(2175, "cute"),
    ParentingInteraction(2176, "interaction"),
    ParentingEducation(2177, "education"),
    ParentingOther(2178, "other"),

    // 健康
    Health(1026, "health", 33),
    HealthScience(2179, "science"),
    HealthRegimen(2180, "regimen"),
    HealthSexes(2181, "sexes"),
    HealthPsychology(2182, "psychology"),
    HealthAsmr(2183, "asmr"),
    HealthOther(2184, "other"),

    // 情感
    Emotion(1027, "emotion", 34),
    EmotionFamily(2185, "family"),
    EmotionRomantic(2186, "romantic"),
    EmotionInterpersonal(2187, "interpersonal"),
    EmotionGrowth(2188, "growth"),

    // 生活兴趣
    LifeJoy(1030, "life_joy", 35),
    LifeJoyLeisure(2198, "leisure"),
    LifeJoyOnSite(2199, "on_site"),
    LifeJoyArtisticProducts(2200, "artistic_products"),
    LifeJoyTrendyToys(2201, "trendy_toys"),
    LifeJoyOther(2202, "other"),

    // 生活经验
    LifeExperience(1031, "life_experience", 36),
    LifeExperienceSkills(2203, "skills"),
    LifeExperienceProcedures(2204, "procedures"),
    LifeExperienceMarriage(2205, "marriage"),

    // 神秘学
    Mysticism(1028, "mysticism", 44),
    MysticismTarot(2189, "tarot"),
    MysticismHoroscope(2190, "horoscope"),
    MysticismMetaphysics(2191, "metaphysics"),
    MysticismHealing(2192, "healing"),
    MysticismOther(2193, "other");

    companion object {
        val dougaList = listOf(
            DougaFanAnime, DougaGarageKit, DougaCosplay, DougaOffline, DougaEditing,
            DougaCommentary, DougaQuickView, DougaVoice, DougaInformation, DougaInterpret,
            DougaVup, DougaTokusatsu, DougaPuppetry, DougaComic, DougaMotion, DougaReaction,
            DougaTutorial, DougaOther
        )
        val gameList = listOf(
            GameRpg, GameMmorpg, GameStandAlone, GameSlg, GameTbs, GameRts, GameMoba, GameStg,
            GameSpg, GameAct, GameMsc, GameSim, GameOtome, GamePuz, GameSandbox, GameOther
        )
        val kichikuList = listOf(
            KichikuGuide, KichikuTheatre, KichikuManualVocaloid, KichikuMad, KichikuOther
        )
        val musicList = listOf(
            MusicOriginal, MusicMv, MusicLive, MusicFanVideos, MusicCover, MusicPerform,
            MusicVocaloid, MusicAiMusic, MusicRadio, MusicTutorial, MusicCommentary, MusicOther
        )
        val danceList = listOf(
            DanceOtaku, DanceHiphop, DanceGestures, DanceStar, DanceChina,
            DanceTutorial, DanceBallet, DanceWota, DanceOther
        )
        val cinephileList = listOf(
            CinephileCommentary, CinephileMontage, CinephileInformation, CinephilePorterage,
            CinephileShortFilm, CinephileAi, CinephileReaction, CinephileOther
        )
        val entList = listOf(
            EntCommentary, EntMontage, EntFansVideo, EntInformation, EntReaction, EntVariety,
            EntOther
        )
        val knowledgeList = listOf(
            KnowledgeExam, KnowledgeLangSkill, KnowledgeCampus, KnowledgeBusiness,
            KnowledgeSocialObservation, KnowledgePolitics, KnowledgeHumanityHistory,
            KnowledgeDesign, KnowledgePsychology, KnowledgeCareer, KnowledgeScience,
            KnowledgeOther
        )
        val techList = listOf(
            TechComputer, TechPhone, TechPad, TechPhotography, TechMachine, TechCreate, TechOther
        )
        val informationList = listOf(
            InformationPolitics, InformationOverseas, InformationSocial, InformationOther
        )
        val foodList = listOf(
            FoodMake, FoodDetective, FoodCommentary, FoodRecord, FoodOther
        )
        val shortplayList = listOf(
            ShortplayPlot, ShortplayLang, ShortplayUpVariety, ShortplayInterview
        )
        val carList = listOf(
            CarCommentary, CarCulture, CarLife, CarTech, CarOther
        )
        val fashionList = listOf(
            FashionMakeup, FashionSkincare, FashionCos, FashionOutfits, FashionAccessories,
            FashionJewelry, FashionTrick, FashionCommentary, FashionOther
        )
        val sportsList = listOf(
            SportsTrend, SportsFootball, SportsBasketball, SportsRunning, SportsKungfu,
            SportsFighting, SportsBadminton, SportsInformation, SportsMatch, SportsOther
        )
        val animalList = listOf(
            AnimalCat, AnimalDog, AnimalReptiles, AnimalScience, AnimalOther
        )
        val vlogList = listOf(
            VlogLife, VlogStudent, VlogCareer, VlogOther
        )
        val paintingList = listOf(
            PaintingAcg, PaintingNoneAcg, PaintingTutorial, PaintingOther
        )
        val aiList = listOf(
            AiTutorial, AiInformation, AiOther
        )
        val homeList = listOf(
            HomeTrade, HomeRenovation, HomeFurniture, HomeAppliances
        )
        val outdoorsList = listOf(
            OutdoorsCamping, OutdoorsHiking, OutdoorsExplore, OutdoorsOther
        )
        val gymList = listOf(
            GymScience, GymTutorial, GymRecord, GymFigure, GymOther
        )
        val handmakeList = listOf(
            HandmakeHandbook, HandmakeLight, HandmakeTraditional, HandmakeRelief, HandmakeDiy,
            HandmakeOther
        )
        val travelList = listOf(
            TravelRecord, TravelStrategy, TravelCity, TravelTransport
        )
        val ruralList = listOf(
            RuralPlanting, RuralFishing, RuralHarvest, RuralTech, RuralLife
        )
        val parentingList = listOf(
            ParentingPregnantCare, ParentingInfantCare, ParentingTalent, ParentingCute,
            ParentingInteraction, ParentingEducation, ParentingOther
        )
        val healthList = listOf(
            HealthScience, HealthRegimen, HealthSexes, HealthPsychology, HealthAsmr,
            HealthOther
        )
        val emotionList = listOf(
            EmotionFamily, EmotionRomantic, EmotionInterpersonal, EmotionGrowth
        )
        val lifeJoyList = listOf(
            LifeJoyLeisure, LifeJoyOnSite, LifeJoyArtisticProducts, LifeJoyTrendyToys, LifeJoyOther
        )
        val lifeExperienceList = listOf(
            LifeExperienceSkills, LifeExperienceProcedures, LifeExperienceMarriage
        )
        val mysticismList = listOf(
            MysticismTarot, MysticismHoroscope, MysticismMetaphysics, MysticismHealing,
            MysticismOther
        )
    }
}