package com.example.registration_login_module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * GrammarDataProvider — Static content provider for all grammar chapters.
 * Each chapter has Learn content, Practice sentences, Translate sentences, and Speaking questions.
 */
public class GrammarDataProvider {

    // ===== CHAPTER DEFINITIONS =====
    public static List<GrammarChapter> getAllChapters() {
        List<GrammarChapter> chapters = new ArrayList<>();
        chapters.add(new GrammarChapter("ch1_prepositions_time", 1, "Time Prepositions", "Master at, on, in", "⏰"));
        chapters.add(new GrammarChapter("ch2_simple_past", 2, "Simple Past Tense", "Talk about yesterday", "⏪"));
        chapters.add(new GrammarChapter("ch3_simple_present", 3, "Simple Present Tense", "Daily habits & facts", "📍"));
        chapters.add(new GrammarChapter("ch4_present_continuous", 4, "Present Continuous", "What's happening now", "🔄"));
        chapters.add(new GrammarChapter("ch5_simple_future", 5, "Simple Future Tense", "Talk about tomorrow", "🔮"));
        chapters.add(new GrammarChapter("ch6_past_continuous", 6, "Past Continuous", "Was/Were doing", "⏳"));
        chapters.add(new GrammarChapter("ch7_articles", 7, "Articles", "A, An, The", "📰"));
        chapters.add(new GrammarChapter("ch8_prepositions_place", 8, "Prepositions of Place", "At, On, In, Under", "📍"));
        chapters.add(new GrammarChapter("ch9_modals", 9, "Modal Verbs", "Can, Could, Should", "💪"));
        chapters.add(new GrammarChapter("ch10_passive", 10, "Passive Voice", "It was done", "🔁"));

        // Initialize missions for each chapter
        for (GrammarChapter ch : chapters) {
            ch.missions = createMissionsForChapter(ch);
        }
        return chapters;
    }

    private static List<GrammarMission> createMissionsForChapter(GrammarChapter ch) {
        List<GrammarMission> missions = new ArrayList<>();
        missions.add(new GrammarMission(ch.chapterId + "_learn", "Learn", GrammarMission.MissionType.LEARN, ch.chapterId, 0));
        missions.add(new GrammarMission(ch.chapterId + "_practice", "Practice", GrammarMission.MissionType.PRACTICE, ch.chapterId, 1));
        missions.add(new GrammarMission(ch.chapterId + "_translate", "Translate", GrammarMission.MissionType.TRANSLATE, ch.chapterId, 2));
        missions.add(new GrammarMission(ch.chapterId + "_speak", "Speak", GrammarMission.MissionType.SPEAK, ch.chapterId, 3));
        return missions;
    }

    // ===== LEARN CONTENT =====
    public static LearnContent getLearnContent(String chapterId) {
        List<LearnSection> sections = new ArrayList<>();
        
        switch (chapterId) {
            case "ch1_prepositions_time":
                sections.add(new LearnSection("Specific Times", "⏰", "Use **AT** for specific times.", Arrays.asList("at 5 o'clock", "at noon", "at midnight", "at sunrise")));
                sections.add(new LearnSection("Days and Dates", "📅", "Use **ON** for days and dates.", Arrays.asList("on Monday", "on 15th August", "on my birthday", "on Christmas Day")));
                sections.add(new LearnSection("Longer Periods", "⏳", "Use **IN** for longer periods like months, years, or seasons.", Arrays.asList("in the morning", "in January", "in 2024", "in summer")));
                return new LearnContent("Time Prepositions", 1, "Learn", 20, "Master at, on, in", sections);
                
            case "ch2_simple_past":
                sections.add(new LearnSection("What is it?", "⏪", "Use Simple Past for **completed actions** in the past.\nStructure: Subject + V2 (past form)", Arrays.asList("I played cricket.", "She went to school.")));
                sections.add(new LearnSection("Regular & Irregular", "🔄", "**Regular verbs** add -ed. **Irregular verbs** change form.", Arrays.asList("play → played", "walk → walked", "go → went", "eat → ate")));
                sections.add(new LearnSection("Negatives & Questions", "❓", "Use **did not + V1** for negatives. Use **Did + Subject + V1** for questions.", Arrays.asList("I did not go.", "Did you go?")));
                return new LearnContent("Simple Past Tense", 1, "Learn", 20, "Talk about yesterday", sections);
                
            case "ch3_simple_present":
                sections.add(new LearnSection("What is it?", "📍", "Use Simple Present for **habits, facts, and routines**.\nStructure: Subject + V1", Arrays.asList("I eat rice.", "She plays cricket.")));
                sections.add(new LearnSection("He / She / It", "👤", "Add **-s/-es** to the verb for third-person singular.", Arrays.asList("He plays.", "She watches.", "It rains.")));
                sections.add(new LearnSection("Negatives & Questions", "❓", "Use **do/does not + V1** for negatives. Use **Do/Does + Subject + V1** for questions.", Arrays.asList("I do not like tea.", "Does he play?")));
                return new LearnContent("Simple Present Tense", 1, "Learn", 20, "Daily habits & facts", sections);
                
            case "ch4_present_continuous":
                sections.add(new LearnSection("What is it?", "🔄", "Use Present Continuous for **actions happening right now**.\nStructure: Subject + is/am/are + V-ing", Arrays.asList("I am reading.", "She is cooking.")));
                sections.add(new LearnSection("Spelling Rules", "📝", "Follow specific rules when adding **-ing**.", Arrays.asList("play → playing", "run → running (double consonant)", "make → making (drop silent e)", "lie → lying (ie → y)")));
                return new LearnContent("Present Continuous", 1, "Learn", 20, "What's happening now", sections);
                
            case "ch5_simple_future":
                sections.add(new LearnSection("What is it?", "🔮", "Use Simple Future for **plans and predictions**.\nStructure: Subject + will + V1", Arrays.asList("I will go.", "She will help.")));
                sections.add(new LearnSection("Going to", "🚶", "Use **going to** for planned actions.", Arrays.asList("I am going to study.", "She is going to cook.")));
                return new LearnContent("Simple Future Tense", 1, "Learn", 20, "Talk about tomorrow", sections);
                
            default:
                sections.add(new LearnSection("Chapter Rules", "📖", "Read the rules clearly before starting the exercises.", Arrays.asList("Rule 1", "Rule 2")));
                return new LearnContent("Chapter Learning", 1, "Learn", 20, "Practice makes perfect.", sections);
        }
    }

    // ===== PRACTICE CONTENT (Hindi → English word bank) =====
    public static List<PracticeSentence> getPracticeContent(String chapterId) {
        List<PracticeSentence> sentences = new ArrayList<>();
        switch (chapterId) {
            case "ch1_prepositions_time":
                sentences.add(new PracticeSentence("मैं सुबह 7 बजे उठता हूँ।", "I wake up at 7 AM", new String[]{"I", "wake", "up", "at", "7", "AM", "the", "on"}));
                sentences.add(new PracticeSentence("बैठक सोमवार को है।", "The meeting is on Monday", new String[]{"The", "meeting", "is", "on", "Monday", "at", "in", "was"}));
                sentences.add(new PracticeSentence("हम जनवरी में छुट्टी पर जाते हैं।", "We go on vacation in January", new String[]{"We", "go", "on", "vacation", "in", "January", "at", "the"}));
                sentences.add(new PracticeSentence("वह रात को 10 बजे सोता है।", "He sleeps at 10 PM", new String[]{"He", "sleeps", "at", "10", "PM", "on", "in", "the"}));
                sentences.add(new PracticeSentence("मेरा जन्मदिन 15 अगस्त को है।", "My birthday is on 15th August", new String[]{"My", "birthday", "is", "on", "15th", "August", "in", "at"}));
                break;
            case "ch2_simple_past":
                sentences.add(new PracticeSentence("मैं कल बाजार गया।", "I went to the market yesterday", new String[]{"I", "went", "to", "the", "market", "yesterday", "go", "will"}));
                sentences.add(new PracticeSentence("उसने रात का खाना बनाया।", "She cooked dinner last night", new String[]{"She", "cooked", "dinner", "last", "night", "cook", "is", "the"}));
                sentences.add(new PracticeSentence("उन्होंने क्रिकेट नहीं खेला।", "They did not play cricket", new String[]{"They", "did", "not", "play", "cricket", "do", "playing", "was"}));
                sentences.add(new PracticeSentence("क्या तुम स्कूल गए?", "Did you go to school", new String[]{"Did", "you", "go", "to", "school", "went", "going", "the"}));
                sentences.add(new PracticeSentence("मैंने एक किताब पढ़ी।", "I read a book", new String[]{"I", "read", "a", "book", "the", "reading", "was", "is"}));
                break;
            case "ch3_simple_present":
                sentences.add(new PracticeSentence("मैं रोज चाय पीता हूँ।", "I drink tea every day", new String[]{"I", "drink", "tea", "every", "day", "drinks", "drank", "the"}));
                sentences.add(new PracticeSentence("वह रोज स्कूल जाती है।", "She goes to school daily", new String[]{"She", "goes", "to", "school", "daily", "go", "went", "the"}));
                sentences.add(new PracticeSentence("वे मांस नहीं खाते।", "They do not eat meat", new String[]{"They", "do", "not", "eat", "meat", "does", "ate", "is"}));
                sentences.add(new PracticeSentence("क्या तुम्हें चाय पसंद है?", "Do you like tea", new String[]{"Do", "you", "like", "tea", "does", "likes", "liked", "is"}));
                sentences.add(new PracticeSentence("सूरज पूरब में उगता है।", "The sun rises in the east", new String[]{"The", "sun", "rises", "in", "the", "east", "rise", "at"}));
                break;
            case "ch4_present_continuous":
                sentences.add(new PracticeSentence("मैं पढ़ रहा हूँ।", "I am reading", new String[]{"I", "am", "reading", "read", "is", "was"}));
                sentences.add(new PracticeSentence("वह सो रही है।", "She is sleeping", new String[]{"She", "is", "sleeping", "sleeps", "was", "sleep"}));
                sentences.add(new PracticeSentence("हम जा रहे हैं।", "We are going", new String[]{"We", "are", "going", "is", "go", "went"}));
                sentences.add(new PracticeSentence("क्या तुम खेल रहे हो?", "Are you playing", new String[]{"Are", "you", "playing", "is", "play", "do"}));
                sentences.add(new PracticeSentence("बारिश हो रही है।", "It is raining", new String[]{"It", "is", "raining", "rain", "are", "the"}));
                break;
            case "ch5_simple_future":
                sentences.add(new PracticeSentence("मैं आऊँगा।", "I will come", new String[]{"I", "will", "come", "came", "is", "going"}));
                sentences.add(new PracticeSentence("वह मदद करेगी।", "She will help", new String[]{"She", "will", "help", "helps", "is", "did"}));
                sentences.add(new PracticeSentence("हम जीतेंगे।", "We will win", new String[]{"We", "will", "win", "won", "are", "is"}));
                sentences.add(new PracticeSentence("क्या तुम जाओगे?", "Will you go", new String[]{"Will", "you", "go", "went", "do", "are"}));
                sentences.add(new PracticeSentence("मैं काम करूँगा।", "I will work", new String[]{"I", "will", "work", "works", "am", "the"}));
                break;
            case "ch6_past_continuous":
                sentences.add(new PracticeSentence("मैं सो रहा था।", "I was sleeping", new String[]{"I", "was", "sleeping", "am", "sleep", "is"}));
                sentences.add(new PracticeSentence("वह रो रही थी।", "She was crying", new String[]{"She", "was", "crying", "is", "cry", "were"}));
                sentences.add(new PracticeSentence("हम खेल रहे थे।", "We were playing", new String[]{"We", "were", "playing", "was", "play", "are"}));
                sentences.add(new PracticeSentence("क्या तुम खा रहे थे?", "Were you eating", new String[]{"Were", "you", "eating", "was", "eat", "are"}));
                sentences.add(new PracticeSentence("वे बात कर रहे थे।", "They were talking", new String[]{"They", "were", "talking", "was", "talk", "is"}));
                break;
            case "ch7_articles":
                sentences.add(new PracticeSentence("यह एक सेब है।", "This is an apple", new String[]{"This", "is", "an", "apple", "a", "the"}));
                sentences.add(new PracticeSentence("सूरज गर्म है।", "The sun is hot", new String[]{"The", "sun", "is", "hot", "a", "an"}));
                sentences.add(new PracticeSentence("मुझे एक पेन चाहिए।", "I need a pen", new String[]{"I", "need", "a", "pen", "an", "the"}));
                sentences.add(new PracticeSentence("वह एक ईमानदार लड़का है।", "He is an honest boy", new String[]{"He", "is", "an", "honest", "boy", "a"}));
                sentences.add(new PracticeSentence("क्या यह किताब है?", "Is this a book", new String[]{"Is", "this", "a", "book", "an", "the"}));
                break;
            case "ch8_prepositions_place":
                sentences.add(new PracticeSentence("किताब मेज पर है।", "The book is on the table", new String[]{"The", "book", "is", "on", "the", "table", "in", "at"}));
                sentences.add(new PracticeSentence("वह कमरे में है।", "He is in the room", new String[]{"He", "is", "in", "the", "room", "on", "at"}));
                sentences.add(new PracticeSentence("कुत्ता पेड़ के नीचे है।", "The dog is under the tree", new String[]{"The", "dog", "is", "under", "the", "tree", "in", "on"}));
                sentences.add(new PracticeSentence("मैं दरवाजे पर हूँ।", "I am at the door", new String[]{"I", "am", "at", "the", "door", "in", "on"}));
                sentences.add(new PracticeSentence("हम छत पर हैं।", "We are on the roof", new String[]{"We", "are", "on", "the", "roof", "in", "at"}));
                break;
            case "ch9_modals":
                sentences.add(new PracticeSentence("मैं दौड़ सकता हूँ।", "I can run", new String[]{"I", "can", "run", "could", "should", "will"}));
                sentences.add(new PracticeSentence("तुम्हें सोना चाहिए।", "You should sleep", new String[]{"You", "should", "sleep", "can", "may", "must"}));
                sentences.add(new PracticeSentence("क्या मैं आ सकता हूँ?", "May I come", new String[]{"May", "I", "come", "can", "should", "must"}));
                sentences.add(new PracticeSentence("हमें जाना होगा।", "We must go", new String[]{"We", "must", "go", "can", "should", "may"}));
                sentences.add(new PracticeSentence("शायद आज बारिश होगी।", "It might rain today", new String[]{"It", "might", "rain", "today", "can", "should"}));
                break;
            case "ch10_passive":
                sentences.add(new PracticeSentence("काम हो गया।", "The work is done", new String[]{"The", "work", "is", "done", "was", "do"}));
                sentences.add(new PracticeSentence("पत्र लिखा गया।", "The letter was written", new String[]{"The", "letter", "was", "written", "is", "write"}));
                sentences.add(new PracticeSentence("कमरा साफ है।", "The room is cleaned", new String[]{"The", "room", "is", "cleaned", "was", "clean"}));
                sentences.add(new PracticeSentence("चाय बन रही है।", "Tea is being made", new String[]{"Tea", "is", "being", "made", "was", "make"}));
                sentences.add(new PracticeSentence("उसे बताया गया था।", "He was told", new String[]{"He", "was", "told", "is", "tell", "telling"}));
                break;
            default:
                sentences.add(new PracticeSentence("वाक्य जल्द आ रहे हैं।", "Content coming soon", new String[]{"Content", "coming", "soon", "the", "is"}));
                break;
        }
        return sentences;
    }

    // ===== TRANSLATE CONTENT (Hindi → English speech) =====
    public static List<TranslateSentence> getTranslateContent(String chapterId) {
        List<TranslateSentence> sentences = new ArrayList<>();
        switch (chapterId) {
            case "ch1_prepositions_time":
                sentences.add(new TranslateSentence("मैं सुबह 6 बजे उठता हूँ।", "I wake up at 6 AM"));
                sentences.add(new TranslateSentence("बैठक शुक्रवार को है।", "The meeting is on Friday"));
                sentences.add(new TranslateSentence("हम गर्मियों में घूमने जाते हैं।", "We go traveling in summer"));
                sentences.add(new TranslateSentence("वह दोपहर को खाना खाता है।", "He eats food at noon"));
                break;
            case "ch2_simple_past":
                sentences.add(new TranslateSentence("मैंने कल खाना खाया।", "I ate food yesterday"));
                sentences.add(new TranslateSentence("वह स्कूल गई।", "She went to school"));
                sentences.add(new TranslateSentence("उन्होंने फिल्म देखी।", "They watched a movie"));
                sentences.add(new TranslateSentence("मैंने एक पत्र लिखा।", "I wrote a letter"));
                break;
            case "ch3_simple_present":
                sentences.add(new TranslateSentence("मैं रोज पानी पीता हूँ।", "I drink water every day"));
                sentences.add(new TranslateSentence("वह क्रिकेट खेलता है।", "He plays cricket"));
                sentences.add(new TranslateSentence("वे स्कूल जाते हैं।", "They go to school"));
                sentences.add(new TranslateSentence("क्या तुम अंग्रेजी बोलते हो?", "Do you speak English"));
                break;
            case "ch4_present_continuous":
                sentences.add(new TranslateSentence("मैं अभी पढ़ रहा हूँ।", "I am reading right now"));
                sentences.add(new TranslateSentence("वह खाना बना रही है।", "She is cooking food"));
                sentences.add(new TranslateSentence("हम टीवी देख रहे हैं।", "We are watching TV"));
                sentences.add(new TranslateSentence("क्या तुम जा रहे हो?", "Are you going"));
                break;
            case "ch5_simple_future":
                sentences.add(new TranslateSentence("मैं कल जाऊँगा।", "I will go tomorrow"));
                sentences.add(new TranslateSentence("वह मेरी मदद करेगी।", "She will help me"));
                sentences.add(new TranslateSentence("हम क्रिकेट खेलेंगे।", "We will play cricket"));
                sentences.add(new TranslateSentence("क्या तुम आओगे?", "Will you come"));
                break;
            case "ch6_past_continuous":
                sentences.add(new TranslateSentence("मैं कल सो रहा था।", "I was sleeping yesterday"));
                sentences.add(new TranslateSentence("वह किताब पढ़ रही थी।", "She was reading a book"));
                sentences.add(new TranslateSentence("हम खेल रहे थे।", "We were playing"));
                sentences.add(new TranslateSentence("क्या तुम टीवी देख रहे थे?", "Were you watching TV"));
                break;
            case "ch7_articles":
                sentences.add(new TranslateSentence("यह एक सेब है।", "This is an apple"));
                sentences.add(new TranslateSentence("वह एक लड़का है।", "He is a boy"));
                sentences.add(new TranslateSentence("सूरज गर्म है।", "The sun is hot"));
                sentences.add(new TranslateSentence("मुझे एक पेन चाहिए।", "I need a pen"));
                break;
            case "ch8_prepositions_place":
                sentences.add(new TranslateSentence("किताब मेज पर है।", "The book is on the table"));
                sentences.add(new TranslateSentence("वह कमरे में है।", "He is in the room"));
                sentences.add(new TranslateSentence("बिल्ली पेड़ के नीचे है।", "The cat is under the tree"));
                sentences.add(new TranslateSentence("मैं दरवाजे पर हूँ।", "I am at the door"));
                break;
            case "ch9_modals":
                sentences.add(new TranslateSentence("मैं तैर सकता हूँ।", "I can swim"));
                sentences.add(new TranslateSentence("तुम्हें जाना चाहिए।", "You should go"));
                sentences.add(new TranslateSentence("क्या मैं अंदर आ सकता हूँ?", "May I come in"));
                sentences.add(new TranslateSentence("वह शायद आज आएगी।", "She might come today"));
                break;
            case "ch10_passive":
                sentences.add(new TranslateSentence("काम किया गया।", "The work was done"));
                sentences.add(new TranslateSentence("पत्र लिखा जा रहा है।", "The letter is being written"));
                sentences.add(new TranslateSentence("कमरा साफ किया गया था।", "The room was cleaned"));
                sentences.add(new TranslateSentence("चाय बन गई है।", "The tea has been made"));
                break;
            default:
                sentences.add(new TranslateSentence("जल्द आ रहा है।", "Coming soon"));
                break;
        }
        return sentences;
    }

    // ===== SPEAKING QUESTIONS =====
    public static List<SpeakQuestion> getSpeakContent(String chapterId) {
        List<SpeakQuestion> questions = new ArrayList<>();
        switch (chapterId) {
            case "ch1_prepositions_time":
                questions.add(new SpeakQuestion("What time do you wake up?", "I wake up at"));
                questions.add(new SpeakQuestion("When is your birthday?", "My birthday is on"));
                questions.add(new SpeakQuestion("In which month do you have vacation?", "I have vacation in"));
                questions.add(new SpeakQuestion("What do you do at night?", "At night I"));
                break;
            case "ch2_simple_past":
                questions.add(new SpeakQuestion("What did you eat yesterday?", "I ate"));
                questions.add(new SpeakQuestion("Where did you go last Sunday?", "I went"));
                questions.add(new SpeakQuestion("Did you study last night?", "Yes I studied"));
                questions.add(new SpeakQuestion("What did you do in the morning?", "I"));
                break;
            case "ch3_simple_present":
                questions.add(new SpeakQuestion("What do you do every morning?", "I"));
                questions.add(new SpeakQuestion("Do you like playing cricket?", "Yes I like"));
                questions.add(new SpeakQuestion("Where does your father work?", "My father works"));
                questions.add(new SpeakQuestion("What do you eat for breakfast?", "I eat"));
                break;
            case "ch4_present_continuous":
                questions.add(new SpeakQuestion("What are you doing right now?", "I am"));
                questions.add(new SpeakQuestion("Is it raining outside?", "it is"));
                questions.add(new SpeakQuestion("What is your friend doing?", "my friend is"));
                questions.add(new SpeakQuestion("Are you learning English?", "yes I am learning"));
                break;
            case "ch5_simple_future":
                questions.add(new SpeakQuestion("What will you do tomorrow?", "I will"));
                questions.add(new SpeakQuestion("Will you go to the market?", "yes I will"));
                questions.add(new SpeakQuestion("Where will you travel next year?", "I will travel"));
                questions.add(new SpeakQuestion("Who will help you?", "will help me"));
                break;
            case "ch6_past_continuous":
                questions.add(new SpeakQuestion("What were you doing at 8 PM yesterday?", "I was"));
                questions.add(new SpeakQuestion("Was it raining yesterday?", "it was"));
                questions.add(new SpeakQuestion("Who were you talking to?", "I was talking to"));
                questions.add(new SpeakQuestion("Were you sleeping in the afternoon?", "yes I was sleeping"));
                break;
            case "ch7_articles":
                questions.add(new SpeakQuestion("What is this in your hand? (an apple)", "this is an apple"));
                questions.add(new SpeakQuestion("Who is the president of India?", "the president is"));
                questions.add(new SpeakQuestion("Do you have a car?", "I have a"));
                questions.add(new SpeakQuestion("Is the sun hot?", "the sun is"));
                break;
            case "ch8_prepositions_place":
                questions.add(new SpeakQuestion("Where is your phone?", "it is on the"));
                questions.add(new SpeakQuestion("Where are you sitting right now?", "I am sitting in"));
                questions.add(new SpeakQuestion("Where is the dog?", "the dog is under"));
                questions.add(new SpeakQuestion("Is someone at the door?", "someone is at the door"));
                break;
            case "ch9_modals":
                questions.add(new SpeakQuestion("Can you swim?", "yes I can"));
                questions.add(new SpeakQuestion("Should I drink water every day?", "you should"));
                questions.add(new SpeakQuestion("May I come in?", "yes you may"));
                questions.add(new SpeakQuestion("What must you do before sleeping?", "I must"));
                break;
            case "ch10_passive":
                questions.add(new SpeakQuestion("Was the homework completed?", "yes it was completed"));
                questions.add(new SpeakQuestion("Is English spoken here?", "yes English is spoken"));
                questions.add(new SpeakQuestion("Who was the book written by?", "it was written by"));
                questions.add(new SpeakQuestion("Has the room been cleaned?", "yes the room has been cleaned"));
                break;
            default:
                questions.add(new SpeakQuestion("Tell me about yourself.", "I am"));
                break;
        }
        return questions;
    }

    // ===== DATA CLASSES =====

    public static class LearnSection {
        public String title;
        public String emoji;
        public String content;
        public List<String> examples;

        public LearnSection(String title, String emoji, String content, List<String> examples) {
            this.title = title;
            this.emoji = emoji;
            this.content = content;
            this.examples = examples;
        }
    }

    public static class LearnContent {
        public String levelTitle;
        public int missionNumber;
        public String missionType;
        public int xpReward;
        public String description;
        public List<LearnSection> sections;

        public LearnContent(String levelTitle, int missionNumber, String missionType, int xpReward, String description, List<LearnSection> sections) {
            this.levelTitle = levelTitle;
            this.missionNumber = missionNumber;
            this.missionType = missionType;
            this.xpReward = xpReward;
            this.description = description;
            this.sections = sections;
        }
    }

    public static class PracticeSentence {
        public String hindiSentence;
        public String correctEnglish;
        public String[] wordBank; // includes correct words + distractors

        public PracticeSentence(String hindi, String english, String[] words) {
            this.hindiSentence = hindi;
            this.correctEnglish = english;
            this.wordBank = words;
        }
    }

    public static class TranslateSentence {
        public String hindiSentence;
        public String expectedEnglish;

        public TranslateSentence(String hindi, String english) {
            this.hindiSentence = hindi;
            this.expectedEnglish = english;
        }
    }

    public static class SpeakQuestion {
        public String question;
        public String expectedKeywords; // keywords to validate in the answer

        public SpeakQuestion(String question, String expectedKeywords) {
            this.question = question;
            this.expectedKeywords = expectedKeywords;
        }
    }
}
