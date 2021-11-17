# Obliczenia peer to peer


> [Struktura sieci](./network_structure.md)\
> [Wiadomości](./messages.md)


| [Struktura sieci](./network_structure.md)|
| [Wiadomości](./messages.md)              |

---

# Protokół komunikacji

## Priorytet stanu

Używany podczas aktualizacji stanu od strony wątku Sieci. Ma zapewnić jednoznaczność stanu na podstawie zbioru przekształceń niezależnie od ich kolejności. Niższy numer - wyższy priorytet:
1. Obliczone
2. Zajęte
3. Wolne

W sytuacji konfliktu w ramach stanu wygrywa ten, którego właścicielem jest Node o niższym id.

## Dołączanie do sieci

1. Tworzy połączenie z [Nodem głównym](#siec-peer-to-peer).
2. Wysyła wiadomość [Pytanie o listę nodów](#pytanie-o-liste-nodow) do [Noda głównego](#siec-peer-to-peer).
3. Otrzymuje wiadomość [Odpowiedź z listą nodów](#odpowiedz-z-lista-nodow).
4. Tworzy połączenie ze wszystkimi pozostałymi [Nodami publicznymi](#siec-peer-to-peer).
5. Rozpoczyna kolejkowanie wiadomości z sieci.
6. Wysyła [Broadcast powitalny](#broadcast-powitalny).
7. Wysyła wiadomość [Pytanie o stan obliczeń](#pytanie-o-stan-obliczen) do losowego Noda.
8. Otrzymuje wiadomość [Odpowiedź ze stanem obliczeń](#odpowiedz-ze-stanem-obliczen) i inicjalizuje [Stan](#watek-stanu).
9. Aplikuje otrzymane zmiany stanu do obiektu [Stanu](#watek-stanu).
10. Node rozpoczyna zwykłą pracę

Protokół jest zawodny w pewnym mało prawdopodobnym scenariuszu. Dlatego potrzebna jest dodatkowa synchronizacja stanu obliczeń między nodami. Zapewnia ją protokół [Końcowa synchronizacja](#koncowa-synchronizacja)

## Heart beat

Potrzebne są dwie struktury danych: 
- słownik ```id_noda : czas przedawnienia ostatniej wiadomości heart beat```. Elementy są dodawane podczas inicjalizacji na podstawie listy nodów, lub gdy node otrzyma [Wiadomość powitalną](#wiadomosc-powitalna). Jest aktualizowana przy każdym otrzymaniu wiadomości [Heart beat](#heart-beat).
- kolejka priorytetowa zaiwerająca pary ```(id_noda, czas przedawnienia danej wiadomości heart beat)```. Priorytetem jest czas przedawnienia (od najwcześniejszych). Przy otrzymaniu wiadomości [Heart beat](#heart-beat) odpowiednia para jest dodawana do kolejki. Element jest ściągany, gdy czas przedawnienia upłynie. Wtedy jest sprawdzany czas przedawnienia w słowniku i jeśli również upłynął, odpowiedni Node jest rozłączany. 


TODO: czy nie może być rozbieżności?

## Rezerwowanie zadania

1. Wątek obliczeń prosi o zadanie.
2. Wątek stanu daje zadanie i informuje wątek sieci o rezerwacji.
3. Wątek sieci wysyła wiadomość [```Zajmuje dane```](#broadcast-zajmuje-dane).
4. Pozostałe nody aktualizują swój stan na podstawie priorytetu i odsyłają odpowiedź [```Ok```](#unicast-ok) ze stanem po aktualizacji.
5. Node otrzymuje odpowiedzi [```Ok```](#unicast-ok).
6. Aktualizuje stan na podstawie odpowiedzi i jeśli to konieczne, przerywa odpowiedni wątek obliczeń oraz na jego miejsce generuje nowy.

## Kończenie zadania

1. Wątek obliczeń po zakończeniu zadania informuje o tym wątek Stanu.
2. Wątek Stanu zmienia stan i informuje o tym wątek Sieci.
3. Wątek Sieci wysyła wiadomość broadcast ```Obliczyłem```.
4. Pozostałe nody aktualizują stan według priorytetu i odsyłają odpowiedź [```Ok```](#unicast-ok) ze stanem po aktualizacji. 
5. Node otrzymuje odpowiedzi [```Ok```](#unicast-ok).
6. Aktualizuje stan na podstawie odpowiedzi i jeśli to konieczne, przerywa odpowiedni wątek obliczeń oraz na jego miejsce generuje nowy.

## Końcowa synchronizacja

1. Kiedy wszystkie zadania w stanie lokalnym staną się zajęte, lub obliczone aktywowany zostaje protokół końcowej synchronizacji. 
2. Wysyła wiadomości [Pytanie o stan zadań](#pytanie-o-stan-zadan) do nodów, które nie ukończyły swojego zadania (według lokalnego stanu). Putanie dla każdego noda dotyczy tylko powiązanych z nim zadań. 
3. Otrzymuje wiadomości [Odpowiedź ze stanem zadań](#odpowiedz-ze-stanem-zadan) i aktualizuje [Stan](#watek-stanu).

# Node

Każdy node składa się z dwóch głównych wątków, które muszą się komunikować: 
- **<font color="blue">Sieci</font>**
- **<font color="green">Obliczeń</font>**

Komunikacja między nimi przebiega z użyciem wzorca ```Active Object```.

<img style="height:300px" src="./img/koncept_watkow.PNG">

Dzięki temu żaden wątek nigdy nie zostaje zablokowany w ramach komunikacji (tak, jak to się dzieje np. przy użyciu wzorca ```Monitor```). ```Active Object``` operuje na strukturze danych obsługującej postęp oraz rezerwacje zadań. Implementacja ```Active object``` zakłada stworzenie kolejnego wątku, dalej nazywanego wątkiem **<font color="red">Planisty</font>**. Dzięki użyciu powyższej architektury, dodanie kolejnego wątku **<font color="green">Obliczeń</font>** nie wymaga żadnych zmian w kodzie. Dodatkowo, ```Active object``` udostępnia interfejs dla wątku **<font color="purple">GUI</font>**, dzięki któremu możliwe jest śledzenie stanu obiczeń. Zależności między wątkami będą zatem wyglądać następująco: 

<img style="height:300px" src="./img/komunikacja_miedzy_watkami.PNG">

# Wątek Sieci

Otrzymuje, przetwarza i wysyła wiadomości w sieci Nodów. Monitoruje ```heart beat``` wszystkich Nodów i zamyka połączenia po przekroczeniu ustalonego czasu bez wiadomości. Wywołuje metody na obiekcie ```Stanu``` i otrzymuje od niego zgłoszenia w ramach subskrypcji. 

Generuje wątek ```Serwera```, który nasłuchuje nadchodzące połączenia. Wątek serwera w momencie otrzymania połączenia generuje wątek ```Połączenia```, który od teraz odbiera dane i kolejkuje je do obsłużenia przez wątek ```Sieci```. Dodatkowo przy uruchamianiu programu, wątek ```Sieci``` tworzy wątki ```Połączenia``` dla każdego publicznego Noda w sieci. 

Wątek ```Sieci```, gdy nie ma nic do roboty, czeka nieaktywnie. Budzony jest przez wątki ```Stanu```, ```Połączenia```, lub ```Serwera``` (niekoniecznie). Wątki ```Serwera``` oraz ```Połączenia``` czekają nieaktywnie na operacjach odpowiednio: akceptacji połączenia oraz czytania ze strumienia (powinno być zaimplementowane w bibliotekach).

Aby wysłać wiadomość, nie potrzeba synchronizacji z wątkiem odbierającym. Potrzebna jest ona natomiast przy kolejkowaniu i odbieraniu wiadomości na drodze wątek ```Połączenia``` -> wątek ```Sieci```. Jest to problem ```NP1C1B```. Rozwiązujemy za pomocą ```LinkedBlockingQueue```.

Wątek ```Heart``` wysyła wiadomość broadcast heart beat do wszystkich nodów w sieci (albo zleca wysłanie wątkowi ```Sieci```).

Kolejną współdzeloną strukturą danych jest tablica aktywnych Nodów, która musi być chroniona. Wątek ```Serwera``` dodaje do niej nowe połączenia, wątki ```Połączenia``` usuwają z niej zapisy po wykryciu, że połączenie zostało urwane. Wymuszone zamknięcie połączenia na skutek nie otrzymania wiadomości ```heart beat``` realizowane przez wątek ```Sieci``` zostanie zauważone przez wątek ```Połączenia```. Problem można uprościć do problemu ```1PNC1B```. Rozwiązujemy za pomocą synchronizowanej tablicy/listy.

Gdy wątek ```Sieci``` otrzyma wiadomość powitalną od noda publicznego i sam jest nodem prywatnym, nawiązuje połączenie z nadawcą. (w pozostałych przypadnkach, połączenie nawiązuje adresat, lub połączenia ma nie być)

# Wątek Stanu

Jest to wątek ```Scheduler``` we wzorcu projektowym ```Active object```. Z tego powodu w tej sekcji opisuję cały wzorzec projektowy, a nie tylko elementy, które działają w tym wątku. 

<img style="height:600px" src="./img/active_object_pl.svg">

*Źródło: https://upload.wikimedia.org/wikipedia/commons/f/f8/Active_object_pl.svg*

## Servant

Klasa Servant jest rdzeniem całego wzorca. To tutaj znajdują się dane, na których operuje ```Active Object```. Dlatego wszystkie publiczne metody dzielą się na dwie kategorie:
- Metody operacyjne - odpowiadają 1:1 wszystkim metodom ```Proxy``` oraz klasom implementującym ```MethodRequest```. Omawiam je w sekcji ```Proxy```.
- Predykaty - używane w implementacji metod ```boolean guard()``` klas implementujących ```MethodRequest```. Udostępniają informacje, dzięki którym ```Scheduler``` może stwierdzić, czy dana metoda może być aktualnie wywołana. 

**W tym przypadku nie zawiera Predykatów, ponieważ [Method Request](#method-request) nie implementuje funkcji ```gruard()```**

Servant operuje na strukturach danych:
- lista zadań i ich stan (wolne, zajęte, obliczone)
- lista/y subskrybentów wzorca ```Observer``` jako obiekty Future zwrócone po użyciu odpowiednich metod. Służą do zawiadamiania innych wątków o zmianie stanu. 

Przykład: ```Wątek Obliczeń``` bierze zadanie i zaczyna liczyć. ```Wątek Sieci``` Zmienia stan tego samego zadania jako zajęte przez inny node na podstawie priorytetu. ```Wątek Obliczeń```, który zasubskrybował o otrzymywanie takiej informacji dostaje nakaz przerwania obliczeń za pośrednictwem zmiany stanu obiektu ```Future```

## Proxy

Jest interfejsem dla stanu obiektu. Jego metody są jedyną drogą komunikacji między innymi wątkami a obiektem. 

- ```Future Proxy.Initialize(myID, progress)```

    Wołane przez wątek ```Sieci```. Inicjuje tablicę postępu oraz informuje o przydzielonym id.

- ```Future Proxy.GetTask()```

    Wołane przez wątek ```Obliczeń```.

    Wybiera pierwsze dostępne zadanie i oznacza jako zajęte przez dany Node. Następnie informuje wątek ```Sieci``` o zajęciu zadania orza zwraca do ```Future``` zadanie.

    Informowanie wątku ```Sieci``` jest zaimplementowane za pomocą wzorca ```Observer``` subskrybującym jest obiekt ```Future```. Informowanie składa się ze zmiany stanu odpowiedniego ```Future``` oraz wybudzenia zainteresowanego wątku.

- ```Future Proxy.ReserveTask(id, task)```

    Wołane przez wątek ```Sieci``` w sytuacji konfliktu przegranego przez noda, dodatkowo informuje wątek ```Obliczeń``` o zmianie rezerwacji.

- ```Future Proxy.CompleteMyTask(task, result)```

    Wołane przez wątek ```Obliczeń``` oznacza zadanie jako wykonane oraz informuje wątek ```Sieci```.

- ```Future Proxy.CompleteTask(id, task, result)```

    Wołane przez wątek ```Sieci```

- Metody subskrybujące/odsubskrybujące

- Metody informacyjne dla UI

## Scheduler

Obsługuje ```Method Request``` pobierane z ```Activation Queue```. Dodatkowo, dodaje, usuwa i informuje subskrybentów wydarzeń. 

Implementuje tworzenie i dodawanie do ```Activation Queue``` obiektów ```Method Request```. 

## Activation Queue

Jako ```Activation Queue``` użyta została kolejka ```LinkedBlockingQueue```. 
Jest ona bezpieczna pod względem współbieżonści. 

Elementy pobieramy blokująco, ponieważ jest to realizowane przez wątek ```Stanu```, który odpowiada jedynie za obsługę skolejkowanych metod.

Elementy również kładziemy blokująco. Przepełnienie występuje jedynie na skutek "niedomagania" wątku ```Stanu``` i jest to sytuacja patologiczna, która nie powinna wystąpić. Dlatego pozostałe wątki zostaną jedynie spowolnione, a nie wpłynie to na poprawność ich działania. Nie nakładamy żadnych warunków dla wywoływania metod, dlatego mamy pewność, że kolejka kiedyś się opróżni (brak możliwości deadlocka). 

## Method Request

Jest w postaci obiektu ```Runnable```. Nie tylko wywołuje metody obiektu ```Servant```, ale również obsługuje subskrypcje. Nie obsługuje warunków wykonania metody (guard). Każdą metodę zawsze można wykonać, ewentualne nieprawidłowości jej wykonania są przekazywane do obiektu ```Future``` i obsługiwane przez zainteresowany wątek.

## Future

Standardowa implementacja

# Wątek Obliczeń

Prosi o zadania obiekt ```Stanu```, oblicza zadanie, informuje o skończeniu zadania.

Jest informowany o zajęciu aktualnie wykonywanego zadania przez inny node. Wtedy przerywa obliczenia i znów prosi o zadanie.

Aby zmilimalizować szansę uśpienia, prosi o nowe zadanie jeszcze przed końcem poprzedniego.

Edge case: 
- Nie ma zadań, ale nie wszystko obliczone - zasypia w oczekiwaniu na powiadomienie o skończeniu wszystkich zadań, lub zwolnieniu zadania
- Wszystko obliczone - zakończ działanie wątku

# Wątek UI

Interfejs użytkownika umożliwia: 
- przerwanie programu (hard, soft)
- wyświetlenie statystyk i postępu
- wyświetlenie wyniku po zakończeniu
- dodawanie/usuwanie wątków obliczeń (tylko soft)

# Workflow systemu

TODO: scenariusze