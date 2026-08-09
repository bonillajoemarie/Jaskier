package com.example.jaskier.songs

data class Song(
    val id: String,
    val title: String,
    val emoji: String,
    val resId: Int,
    // Traditional public-domain lyric lines for the karaoke view. Songs whose
    // exact recorded lyrics we can't verify get a sing-along prompt instead.
    val lyrics: List<String> = emptyList(),
)

// U.S. Department of State "Sing Out Loud Children's Songs" — public domain
// (works of the U.S. federal government). See ATTRIBUTIONS.md.
val Songs = listOf(
    Song(
        "alphabet", "The Alphabet Song", "🔤", R.raw.song_alphabet,
        listOf(
            "A  B  C  D  E  F  G",
            "H  I  J  K  L  M  N  O  P",
            "Q  R  S,  T  U  V",
            "W,  X,  Y and Z",
            "Now I know my ABCs",
            "Next time won't you sing with me?",
        ),
    ),
    Song(
        "one_two", "One, Two, Buckle My Shoe", "🔢", R.raw.song_one_two,
        listOf(
            "One, two, buckle my shoe",
            "Three, four, shut the door",
            "Five, six, pick up sticks",
            "Seven, eight, lay them straight",
            "Nine, ten, a big fat hen!",
        ),
    ),
    Song(
        "twinkle", "Twinkle, Twinkle, Little Star", "⭐", R.raw.song_twinkle,
        listOf(
            "Twinkle, twinkle, little star",
            "How I wonder what you are",
            "Up above the world so high",
            "Like a diamond in the sky",
            "Twinkle, twinkle, little star",
            "How I wonder what you are",
        ),
    ),
    Song(
        "bingo", "B-I-N-G-O", "🐶", R.raw.song_bingo,
        listOf(
            "There was a farmer had a dog",
            "And Bingo was his name-o",
            "B - I - N - G - O",
            "B - I - N - G - O",
            "B - I - N - G - O",
            "And Bingo was his name-o!",
        ),
    ),
    Song(
        "head_shoulders", "Head, Shoulders, Knees & Toes", "🙆", R.raw.song_head_shoulders,
        listOf(
            "Head, shoulders, knees, and toes",
            "Knees and toes",
            "Head, shoulders, knees, and toes",
            "Knees and toes",
            "And eyes and ears and mouth and nose",
            "Head, shoulders, knees, and toes",
            "Knees and toes!",
        ),
    ),
    Song(
        "hokey", "Hokey Pokey", "🕺", R.raw.song_hokey,
        listOf(
            "You put your right hand in",
            "You put your right hand out",
            "You put your right hand in",
            "And you shake it all about",
            "You do the hokey pokey",
            "And you turn yourself around",
            "That's what it's all about!",
        ),
    ),
    Song(
        "happy", "The Happy Song", "😊", R.raw.song_happy,
        listOf(
            "Clap your hands!",
            "Stomp your feet!",
            "Sing along and feel the beat!",
            "La la la la la!",
        ),
    ),
    Song(
        "hickory", "Hickory Dickory Dock", "🕰️", R.raw.song_hickory,
        listOf(
            "Hickory dickory dock",
            "The mouse ran up the clock",
            "The clock struck one",
            "The mouse ran down",
            "Hickory dickory dock",
        ),
    ),
    Song(
        "mary_lamb", "Mary Had a Little Lamb", "🐑", R.raw.song_mary_lamb,
        listOf(
            "Mary had a little lamb",
            "Little lamb, little lamb",
            "Mary had a little lamb",
            "Its fleece was white as snow",
            "And everywhere that Mary went",
            "Mary went, Mary went",
            "Everywhere that Mary went",
            "The lamb was sure to go",
        ),
    ),
    Song(
        "sleeping", "Are You Sleeping?", "😴", R.raw.song_sleeping,
        listOf(
            "Are you sleeping?",
            "Are you sleeping?",
            "Brother John, Brother John",
            "Morning bells are ringing",
            "Morning bells are ringing",
            "Ding, dang, dong",
            "Ding, dang, dong",
        ),
    ),
    Song(
        "teapot", "I'm a Little Teapot", "🫖", R.raw.song_teapot,
        listOf(
            "I'm a little teapot",
            "Short and stout",
            "Here is my handle",
            "Here is my spout",
            "When I get all steamed up",
            "Hear me shout",
            "Tip me over and pour me out!",
        ),
    ),
    Song(
        "river_woods", "Over the River & Through the Woods", "🛷", R.raw.song_river_woods,
        listOf(
            "Over the river and through the woods",
            "To Grandmother's house we go",
            "The horse knows the way",
            "To carry the sleigh",
            "Through the white and drifted snow",
        ),
    ),
    Song(
        "hush_baby", "Hush Little Baby", "🌙", R.raw.song_hush_baby,
        listOf(
            "Hush, little baby, don't say a word",
            "Mama's gonna buy you a mockingbird",
            "And if that mockingbird won't sing",
            "Mama's gonna buy you a diamond ring",
        ),
    ),
)

data class AnimalSound(
    val id: String,
    val name: String,
    val resId: Int,
)

// Recordings from Wikimedia Commons contributors — CC BY-SA / public domain.
// See ATTRIBUTIONS.md for per-file credits.
val AnimalSounds = listOf(
    AnimalSound("dog", "Dog", R.raw.animal_dog),
    AnimalSound("cat", "Cat", R.raw.animal_cat),
    AnimalSound("cow", "Cow", R.raw.animal_cow),
    AnimalSound("duck", "Duck", R.raw.animal_duck),
    AnimalSound("sheep", "Sheep", R.raw.animal_sheep),
    AnimalSound("rooster", "Rooster", R.raw.animal_rooster),
)
